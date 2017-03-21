/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "code_ir.h"
#include "common.h"
#include "dex_bytecode.h"
#include "dex_format.h"
#include "dex_ir.h"
#include "dex_leb128.h"
#include "bytecode_encoder.h"
#include "debuginfo_encoder.h"
#include "tryblocks_encoder.h"

#include <assert.h>
#include <string.h>
#include <algorithm>
#include <cstdlib>
#include <type_traits>
#include <vector>

namespace lir {

void CodeIr::Assemble() {
  auto ir_code = ir_method_->code;
  CHECK(ir_code != nullptr);

  // new .dex bytecode
  //
  // NOTE: this must be done before the debug information and
  //   try/catch blocks since here is where we update the final offsets
  //
  BytecodeEncoder bytecode_encoder(instructions);
  bytecode_encoder.Encode(ir_code, dex_ir_);

  // debug information
  if (ir_code->debug_info != nullptr) {
    DebugInfoEncoder dbginfo_encoder(instructions);
    dbginfo_encoder.Encode(ir_method_, dex_ir_);
  }

  // try/catch blocks
  TryBlocksEncoder try_blocks_encoder(instructions);
  try_blocks_encoder.Encode(ir_code, dex_ir_);
}

void CodeIr::DissasembleTryBlocks(const ir::Code* ir_code) {
  int nextTryBlockId = 1;
  for (const auto& tryBlock : ir_code->try_blocks) {
    auto try_block_begin = Alloc<TryBlockBegin>();
    try_block_begin->id = nextTryBlockId++;
    try_block_begin->offset = tryBlock.start_addr;

    auto try_block_end = Alloc<TryBlockEnd>();
    try_block_end->try_begin = try_block_begin;
    try_block_end->offset = tryBlock.start_addr + tryBlock.insn_count;

    // parse the catch handlers
    const dex::u1* ptr =
        ir_code->catch_handlers.ptr<dex::u1>() + tryBlock.handler_off;
    int catchCount = dex::ReadSLeb128(&ptr);

    for (int catchIndex = 0; catchIndex < std::abs(catchCount); ++catchIndex) {
      CatchHandler handler = {};

      // type
      dex::u4 type_index = dex::ReadULeb128(&ptr);
      handler.ir_type = dex_ir_->types_map[type_index];
      CHECK(handler.ir_type != nullptr);

      // address
      dex::u4 address = dex::ReadULeb128(&ptr);
      handler.label = GetLabel(address);

      try_block_end->handlers.push_back(handler);
    }

    // catch_all handler?
    //
    // NOTE: this is used to generate code for the "finally" blocks
    //  (see Java Virtual Machine Specification - 3.13 "Compiling finally")
    //
    if (catchCount < 1) {
      dex::u4 address = dex::ReadULeb128(&ptr);
      try_block_end->catch_all = GetLabel(address);
    }

    // we should have at least one handler
    CHECK(!try_block_end->handlers.empty() ||
          try_block_end->catch_all != nullptr);

    try_begins_.push_back(try_block_begin);
    try_ends_.push_back(try_block_end);
  }
}

void CodeIr::DissasembleDebugInfo(const ir::DebugInfo* ir_debug_info) {
  if (ir_debug_info == nullptr) {
    return;
  }

  // debug info state machine registers
  dex::u4 address = 0;
  int line = ir_debug_info->line_start;
  ir::String* source_file = ir_method_->parent_class->source_file;

  // header
  if (!ir_debug_info->param_names.empty()) {
    auto dbg_header = Alloc<DbgInfoHeader>();
    dbg_header->param_names = ir_debug_info->param_names;
    dbg_header->offset = 0;
    dbg_annotations_.push_back(dbg_header);
  }

  // initial source file
  {
    auto annotation = Alloc<DbgInfoAnnotation>(dex::DBG_SET_FILE);
    annotation->offset = 0;
    annotation->operands.push_back(Alloc<String>(
        source_file, source_file ? source_file->orig_index : dex::kNoIndex));
    dbg_annotations_.push_back(annotation);
  }

  // initial line number - redundant?
  {
    auto annotation = Alloc<DbgInfoAnnotation>(dex::DBG_ADVANCE_LINE);
    annotation->offset = 0;
    annotation->operands.push_back(Alloc<LineNumber>(line));
    dbg_annotations_.push_back(annotation);
  }

  // debug info annotations
  const dex::u1* ptr = ir_debug_info->data.ptr<dex::u1>();
  dex::u1 opcode = 0;
  while ((opcode = *ptr++) != dex::DBG_END_SEQUENCE) {
    DbgInfoAnnotation* annotation = nullptr;

    switch (opcode) {
      case dex::DBG_ADVANCE_PC:
        // addr_diff
        address += dex::ReadULeb128(&ptr);
        break;

      case dex::DBG_ADVANCE_LINE:
        // line_diff
        line += dex::ReadSLeb128(&ptr);
        WEAK_CHECK(line > 0);
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        annotation->operands.push_back(Alloc<LineNumber>(line));
        break;

      case dex::DBG_START_LOCAL: {
        annotation = Alloc<DbgInfoAnnotation>(opcode);

        // register_num
        annotation->operands.push_back(Alloc<VReg>(dex::ReadULeb128(&ptr)));

        // name
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetString(name_index));

        // type
        dex::u4 type_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetType(type_index));
      } break;

      case dex::DBG_START_LOCAL_EXTENDED: {
        annotation = Alloc<DbgInfoAnnotation>(opcode);

        // register_num
        annotation->operands.push_back(Alloc<VReg>(dex::ReadULeb128(&ptr)));

        // name
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetString(name_index));

        // type
        dex::u4 type_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetType(type_index));

        // signature
        dex::u4 sig_index = dex::ReadULeb128(&ptr) - 1;
        annotation->operands.push_back(GetString(sig_index));
      } break;

      case dex::DBG_END_LOCAL:
      case dex::DBG_RESTART_LOCAL:
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        // register_num
        annotation->operands.push_back(Alloc<VReg>(dex::ReadULeb128(&ptr)));
        break;

      case dex::DBG_SET_PROLOGUE_END:
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        break;

      case dex::DBG_SET_EPILOGUE_BEGIN:
        annotation = Alloc<DbgInfoAnnotation>(opcode);
        break;

      case dex::DBG_SET_FILE: {
        annotation = Alloc<DbgInfoAnnotation>(opcode);

        // source file name
        dex::u4 name_index = dex::ReadULeb128(&ptr) - 1;
        source_file = (name_index == dex::kNoIndex)
                          ? nullptr
                          : dex_ir_->strings_map[name_index];
        annotation->operands.push_back(Alloc<String>(source_file, name_index));
      } break;

      default: {
        int adjusted_opcode = opcode - dex::DBG_FIRST_SPECIAL;
        line += dex::DBG_LINE_BASE + (adjusted_opcode % dex::DBG_LINE_RANGE);
        address += (adjusted_opcode / dex::DBG_LINE_RANGE);
        WEAK_CHECK(line > 0);
        annotation = Alloc<DbgInfoAnnotation>(dex::DBG_ADVANCE_LINE);
        annotation->operands.push_back(Alloc<LineNumber>(line));
      } break;
    }

    if (annotation != nullptr) {
      annotation->offset = address;
      dbg_annotations_.push_back(annotation);
    }
  }
}

void CodeIr::DissasembleBytecode(const ir::Code* ir_code) {
  const dex::u2* begin = ir_code->instructions.begin();
  const dex::u2* end = ir_code->instructions.end();
  const dex::u2* ptr = begin;

  while (ptr < end) {
    auto isize = dex::GetWidthFromBytecode(ptr);
    CHECK(isize > 0);

    dex::u4 offset = ptr - begin;

    Instruction* instr = nullptr;
    switch (*ptr) {
      case dex::kPackedSwitchSignature:
        instr = DecodePackedSwitch(ptr, offset);
        break;

      case dex::kSparseSwitchSignature:
        instr = DecodeSparseSwitch(ptr, offset);
        break;

      case dex::kArrayDataSignature:
        instr = DecodeArrayData(ptr, offset);
        break;

      default:
        instr = DecodeBytecode(ptr, offset);
        break;
    }

    instr->offset = offset;
    instructions.push_back(instr);
    ptr += isize;
  }
  CHECK(ptr == end);
}

void CodeIr::FixupSwitches() {
  const dex::u2* begin = ir_method_->code->instructions.begin();

  // packed switches
  for (auto& fixup : packed_switches_) {
    FixupPackedSwitch(fixup.second.instr, fixup.second.baseOffset,
                      begin + fixup.first);
  }

  // sparse switches
  for (auto& fixup : sparse_switches_) {
    FixupSparseSwitch(fixup.second.instr, fixup.second.baseOffset,
                      begin + fixup.first);
  }
}

// merge a set of extra instructions into the instruction list
template <class I_LIST, class E_LIST>
static void MergeInstructions(I_LIST& instructions, const E_LIST& extra) {

  // the extra instructins must be sorted by offset
  CHECK(std::is_sorted(extra.begin(), extra.end(),
                        [](const Instruction* a, const Instruction* b) {
                          return a->offset < b->offset;
                        }));

  auto instrIt = instructions.begin();
  auto extraIt = extra.begin();

  while (extraIt != extra.end()) {
    if (instrIt == instructions.end() ||
        (*extraIt)->offset == (*instrIt)->offset) {
      instructions.insert(instrIt, *extraIt);
      ++extraIt;
    } else {
      ++instrIt;
    }
  }
}

void CodeIr::Dissasemble() {
  nodes_.clear();
  labels_.clear();

  try_begins_.clear();
  try_ends_.clear();
  dbg_annotations_.clear();
  packed_switches_.clear();
  sparse_switches_.clear();

  auto ir_code = ir_method_->code;
  if (ir_code == nullptr) {
    return;
  }

  // decode the .dex bytecodes
  DissasembleBytecode(ir_code);

  // try/catch blocks
  DissasembleTryBlocks(ir_code);

  // debug information
  DissasembleDebugInfo(ir_code->debug_info);

  // fixup switches
  FixupSwitches();

  // assign label ids
  std::vector<Label*> tmp_labels;
  int nextLabelId = 1;
  for (auto& label : labels_) {
    label.second->id = nextLabelId++;
    tmp_labels.push_back(label.second);
  }

  // merge the labels into the instructions stream
  MergeInstructions(instructions, dbg_annotations_);
  MergeInstructions(instructions, try_begins_);
  MergeInstructions(instructions, tmp_labels);
  MergeInstructions(instructions, try_ends_);
}

PackedSwitch* CodeIr::DecodePackedSwitch(const dex::u2* /*ptr*/,
                                         dex::u4 offset) {
  // actual decoding is delayed to FixupPackedSwitch()
  // (since the label offsets are relative to the referring
  //  instruction, not the switch data)
  CHECK(offset % 2 == 0);
  auto& instr = packed_switches_[offset].instr;
  CHECK(instr == nullptr);
  instr = Alloc<PackedSwitch>();
  return instr;
}

void CodeIr::FixupPackedSwitch(PackedSwitch* instr, dex::u4 baseOffset,
                               const dex::u2* ptr) {
  CHECK(instr->targets.empty());

  auto dexPackedSwitch = reinterpret_cast<const dex::PackedSwitch*>(ptr);
  CHECK(dexPackedSwitch->ident == dex::kPackedSwitchSignature);

  instr->first_key = dexPackedSwitch->first_key;
  for (dex::u2 i = 0; i < dexPackedSwitch->size; ++i) {
    instr->targets.push_back(
        GetLabel(baseOffset + dexPackedSwitch->targets[i]));
  }
}

SparseSwitch* CodeIr::DecodeSparseSwitch(const dex::u2* /*ptr*/,
                                         dex::u4 offset) {
  // actual decoding is delayed to FixupSparseSwitch()
  // (since the label offsets are relative to the referring
  //  instruction, not the switch data)
  CHECK(offset % 2 == 0);
  auto& instr = sparse_switches_[offset].instr;
  CHECK(instr == nullptr);
  instr = Alloc<SparseSwitch>();
  return instr;
}

void CodeIr::FixupSparseSwitch(SparseSwitch* instr, dex::u4 baseOffset,
                               const dex::u2* ptr) {
  CHECK(instr->switch_cases.empty());

  auto dexSparseSwitch = reinterpret_cast<const dex::SparseSwitch*>(ptr);
  CHECK(dexSparseSwitch->ident == dex::kSparseSwitchSignature);

  auto& data = dexSparseSwitch->data;
  auto& size = dexSparseSwitch->size;

  for (dex::u2 i = 0; i < size; ++i) {
    SparseSwitch::SwitchCase switchCase = {};
    switchCase.key = data[i];
    switchCase.target = GetLabel(baseOffset + data[i + size]);
    instr->switch_cases.push_back(switchCase);
  }
}

ArrayData* CodeIr::DecodeArrayData(const dex::u2* ptr, dex::u4 offset) {
  auto dexArrayData = reinterpret_cast<const dex::ArrayData*>(ptr);
  CHECK(dexArrayData->ident == dex::kArrayDataSignature);
  CHECK(offset % 2 == 0);

  auto instr = Alloc<ArrayData>();
  instr->data = slicer::MemView(ptr, dex::GetWidthFromBytecode(ptr) * 2);
  return instr;
}

Bytecode* CodeIr::DecodeBytecode(const dex::u2* ptr, dex::u4 offset) {
  auto dexInstr = dex::DecodeInstruction(ptr);

  auto instr = Alloc<Bytecode>();
  instr->opcode = dexInstr.opcode;

  auto indexType = dex::GetIndexTypeFromOpcode(dexInstr.opcode);

  switch (dex::GetFormatFromOpcode(dexInstr.opcode)) {
    case dex::kFmt10x:  // op
      break;

    case dex::kFmt12x:  // op vA, vB
    case dex::kFmt22x:  // op vAA, vBBBB
    case dex::kFmt32x:  // op vAAAA, vBBBB
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(Alloc<VReg>(dexInstr.vB));
      break;

    case dex::kFmt11n:  // op vA, #+B
    case dex::kFmt21s:  // op vAA, #+BBBB
    case dex::kFmt31i:  // op vAA, #+BBBBBBBB
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(Alloc<Const32>(dexInstr.vB));
      break;

    case dex::kFmt11x:  // op vAA
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      break;

    case dex::kFmt10t:  // op +AA
    case dex::kFmt20t:  // op +AAAA
    case dex::kFmt30t:  // op +AAAAAAAA
    {
      auto label = GetLabel(offset + dex::s4(dexInstr.vA));
      instr->operands.push_back(Alloc<CodeLocation>(label));
    } break;

    case dex::kFmt21t:  // op vAA, +BBBB
    case dex::kFmt31t:  // op vAA, +BBBBBBBB
    {
      dex::u4 targetOffset = offset + dex::s4(dexInstr.vB);
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      auto label = GetLabel(targetOffset);
      instr->operands.push_back(Alloc<CodeLocation>(label));

      if (dexInstr.opcode == dex::OP_PACKED_SWITCH) {
        label->aligned = true;
        dex::u4& baseOffset = packed_switches_[targetOffset].baseOffset;
        CHECK(baseOffset == kInvalidOffset);
        baseOffset = offset;
      } else if (dexInstr.opcode == dex::OP_SPARSE_SWITCH) {
        label->aligned = true;
        dex::u4& baseOffset = sparse_switches_[targetOffset].baseOffset;
        CHECK(baseOffset == kInvalidOffset);
        baseOffset = offset;
      } else if (dexInstr.opcode == dex::OP_FILL_ARRAY_DATA) {
        label->aligned = true;
      }
    } break;

    case dex::kFmt23x:  // op vAA, vBB, vCC
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(Alloc<VReg>(dexInstr.vB));
      instr->operands.push_back(Alloc<VReg>(dexInstr.vC));
      break;

    case dex::kFmt22t:  // op vA, vB, +CCCC
    {
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(Alloc<VReg>(dexInstr.vB));
      auto label = GetLabel(offset + dex::s4(dexInstr.vC));
      instr->operands.push_back(Alloc<CodeLocation>(label));
    } break;

    case dex::kFmt22b:  // op vAA, vBB, #+CC
    case dex::kFmt22s:  // op vA, vB, #+CCCC
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(Alloc<VReg>(dexInstr.vB));
      instr->operands.push_back(Alloc<Const32>(dexInstr.vC));
      break;

    case dex::kFmt22c:  // op vA, vB, thing@CCCC
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(Alloc<VReg>(dexInstr.vB));
      instr->operands.push_back(GetIndexedOperand(indexType, dexInstr.vC));
      break;

    case dex::kFmt21c:  // op vAA, thing@BBBB
    case dex::kFmt31c:  // op vAA, string@BBBBBBBB
      instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
      instr->operands.push_back(GetIndexedOperand(indexType, dexInstr.vB));
      break;

    case dex::kFmt35c:  // op {vC,vD,vE,vF,vG}, thing@BBBB
    {
      CHECK(dexInstr.vA <= 5);
      auto vreg_list = Alloc<VRegList>();
      for (dex::u4 i = 0; i < dexInstr.vA; ++i) {
        vreg_list->registers.push_back(dexInstr.arg[i]);
      }
      instr->operands.push_back(vreg_list);
      instr->operands.push_back(GetIndexedOperand(indexType, dexInstr.vB));
    } break;

    case dex::kFmt3rc:  // op {vCCCC .. v(CCCC+AA-1)}, thing@BBBB
    {
      auto vreg_range = Alloc<VRegRange>(dexInstr.vC, dexInstr.vA);
      instr->operands.push_back(vreg_range);
      instr->operands.push_back(GetIndexedOperand(indexType, dexInstr.vB));
    } break;

    case dex::kFmt21h:  // op vAA, #+BBBB0000[00000000]
      switch (dexInstr.opcode) {
        case dex::OP_CONST_HIGH16:
          instr->operands.push_back(Alloc<VReg>(dexInstr.vA));
          instr->operands.push_back(Alloc<Const32>(dexInstr.vB << 16));
          break;

        case dex::OP_CONST_WIDE_HIGH16:
          instr->operands.push_back(Alloc<VRegPair>(dexInstr.vA));
          instr->operands.push_back(Alloc<Const64>(dex::u8(dexInstr.vB) << 48));
          break;

        default:
          FATAL("Unexpected opcode 0x%02x", dexInstr.opcode);
      }
      break;

    case dex::kFmt51l:  // op vAA, #+BBBBBBBBBBBBBBBB
      instr->operands.push_back(Alloc<VRegPair>(dexInstr.vA));
      instr->operands.push_back(Alloc<Const64>(dexInstr.vB_wide));
      break;

    default:
      FATAL("Unexpected bytecode format (opcode 0x%02x)", dexInstr.opcode);
  }

  return instr;
}

// Get a indexed object (string, field, ...)
// (index must be valid != kNoIndex)
IndexedOperand* CodeIr::GetIndexedOperand(dex::InstructionIndexType indexType,
                                          dex::u4 index) {
  CHECK(index != dex::kNoIndex);
  switch (indexType) {
    case dex::kIndexStringRef:
      return Alloc<String>(dex_ir_->strings_map[index], index);

    case dex::kIndexTypeRef:
      return Alloc<Type>(dex_ir_->types_map[index], index);

    case dex::kIndexFieldRef:
      return Alloc<Field>(dex_ir_->fields_map[index], index);

    case dex::kIndexMethodRef:
      return Alloc<Method>(dex_ir_->methods_map[index], index);

    default:
      FATAL("Unexpected index type 0x%02x", indexType);
  }
}

// Get a type based on its index (potentially kNoIndex)
Type* CodeIr::GetType(dex::u4 index) {
  auto ir_type = (index == dex::kNoIndex) ? nullptr : dex_ir_->types_map[index];
  return Alloc<Type>(ir_type, index);
}

// Get a string based on its index (potentially kNoIndex)
String* CodeIr::GetString(dex::u4 index) {
  auto ir_string = (index == dex::kNoIndex) ? nullptr : dex_ir_->strings_map[index];
  return Alloc<String>(ir_string, index);
}

// Get en existing, or new label for a particular offset
Label* CodeIr::GetLabel(dex::u4 offset) {
  auto& p = labels_[offset];
  if (p == nullptr) {
    p = Alloc<Label>(offset);
  }
  ++p->refCount;
  return p;
}

}  // namespace lir
