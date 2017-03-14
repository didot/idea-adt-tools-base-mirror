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

#include "dissasembler.h"

#include <stdio.h>
#include <cinttypes>
#include <cmath>

bool PrintCodeIrVisitor::Visit(lir::Bytecode* bytecode) {
  printf("\t%5u| %s", bytecode->offset, dex::GetOpcodeName(bytecode->opcode));
  bool first = true;
  for (auto op : bytecode->operands) {
    printf(first ? " " : ", ");
    op->Accept(this);
    first = false;
  }
  printf("\n");
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::PackedSwitch* packed_switch) {
  printf("\t%5u| packed-switch-payload\n", packed_switch->offset);
  int key = packed_switch->first_key;
  for (auto target : packed_switch->targets) {
    printf("\t\t%5d: ", key++);
    printf("Label_%d", target->id);
    printf("\n");
  }
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::SparseSwitch* sparse_switch) {
  printf("\t%5u| sparse-switch-payload\n", sparse_switch->offset);
  for (auto& switchCase : sparse_switch->switch_cases) {
    printf("\t\t%5d: ", switchCase.key);
    printf("Label_%d", switchCase.target->id);
    printf("\n");
  }
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::ArrayData* array_data) {
  printf("\t%5u| fill-array-data-payload\n", array_data->offset);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::CodeLocation* target) {
  printf("Label_%d", target->label->id);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::Const32* const32) {
  printf("#%+d (0x%08x | ", const32->u.s4_value, const32->u.u4_value);
  if (std::isnan(const32->u.float_value)) {
    printf("NaN)");
  } else {
    printf("%f)", const32->u.float_value);
  }
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::Const64* const64) {
  printf("#%+" PRId64 " (0x%016" PRIx64 " | ", const64->u.s8_value, const64->u.u8_value);
  if (std::isnan(const64->u.double_value)) {
    printf("NaN)");
  } else {
    printf("%f)", const64->u.double_value);
  }
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::VReg* vreg) {
  printf("v%d", vreg->reg);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::VRegPair* vreg_pair) {
  printf("v%d/v%d", vreg_pair->base_reg, vreg_pair->base_reg + 1);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::VRegList* vreg_list) {
  bool first = true;
  printf("{");
  for (auto reg : vreg_list->registers) {
    printf("%sv%d", (first ? "" : ","), reg);
    first = false;
  }
  printf("}");
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::VRegRange* vreg_range) {
  if (vreg_range->count == 0) {
    printf("{}");
  } else {
    printf("{v%d..v%d}", vreg_range->base_reg,
           vreg_range->base_reg + vreg_range->count - 1);
  }
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::String* string) {
  if (string->index == dex::kNoIndex) {
    printf("<null>");
    return true;
  }
  auto ir_string = dex_ir_->strings_map[string->index];
  printf("\"");
  for (const char* p = ir_string->c_str(); *p != '\0'; ++p) {
    if (::isprint(*p)) {
      printf("%c", *p);
    } else {
      switch (*p) {
        case '\'': printf("\\'");   break;
        case '\"': printf("\\\"");  break;
        case '\?': printf("\\?");   break;
        case '\\': printf("\\\\");  break;
        case '\a': printf("\\a");   break;
        case '\b': printf("\\b");   break;
        case '\f': printf("\\f");   break;
        case '\n': printf("\\n");   break;
        case '\r': printf("\\r");   break;
        case '\t': printf("\\t");   break;
        case '\v': printf("\\v");   break;
        default:
          printf("\\x%02x", *p);
          break;
      }
    }
  }
  printf("\"");
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::Type* type) {
  CHECK(type->index != dex::kNoIndex);
  auto ir_type = dex_ir_->types_map[type->index];
  printf("%s", ir_type->Decl().c_str());
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::Field* field) {
  CHECK(field->index != dex::kNoIndex);
  auto ir_field = dex_ir_->fields_map[field->index];
  printf("%s.%s", ir_field->parent->Decl().c_str(), ir_field->name->c_str());
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::Method* method) {
  CHECK(method->index != dex::kNoIndex);
  auto ir_method = dex_ir_->methods_map[method->index];
  printf("%s.%s", ir_method->parent->Decl().c_str(), ir_method->name->c_str());
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::Label* label) {
  printf("Label_%d:\n", label->id);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::TryBlockBegin* try_begin) {
  printf("\t.try_begin_%d\n", try_begin->id);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::TryBlockEnd* try_end) {
  printf("\t.try_end_%d\n", try_end->try_begin->id);
  for (const auto& handler : try_end->handlers) {
    printf("\t  catch(%s) : Label_%d\n", handler.ir_type->Decl().c_str(),
           handler.label->id);
  }
  if (try_end->catch_all != nullptr) {
    printf("\t  catch(...) : Label_%d\n", try_end->catch_all->id);
  }
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::LineNumber* line_number) {
  printf("%d", line_number->line);
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::DbgInfoHeader* dbg_header) {
  printf("\t.params");
  bool first = true;
  for (auto paramName : dbg_header->param_names) {
    printf(first ? " " : ", ");
    printf("\"%s\"", paramName ? paramName->c_str() : "?");
    first = false;
  }
  printf("\n");
  return true;
}

bool PrintCodeIrVisitor::Visit(lir::DbgInfoAnnotation* annotation) {
  const char* name = ".dbg_???";
  switch (annotation->dbg_opcode) {
    case dex::DBG_START_LOCAL:
      name = ".local";
      break;
    case dex::DBG_START_LOCAL_EXTENDED:
      name = ".local_ex";
      break;
    case dex::DBG_END_LOCAL:
      name = ".end_local";
      break;
    case dex::DBG_RESTART_LOCAL:
      name = ".restart_local";
      break;
    case dex::DBG_SET_PROLOGUE_END:
      name = ".prologue_end";
      break;
    case dex::DBG_SET_EPILOGUE_BEGIN:
      name = ".epilogue_begin";
      break;
    case dex::DBG_ADVANCE_LINE:
      name = ".line";
      break;
    case dex::DBG_SET_FILE:
      name = ".src";
      break;
  }
  printf("\t%s", name);

  bool first = true;
  for (auto op : annotation->operands) {
    printf(first ? " " : ", ");
    op->Accept(this);
    first = false;
  }

  printf("\n");
  return true;
}

void DexDissasembler::DumpAllMethods() const {
  for (auto& ir_method : dex_ir_->encoded_methods) {
    DumpMethod(ir_method.get());
  }
}

void DexDissasembler::DumpMethod(ir::EncodedMethod* ir_method) const {
  printf("\nmethod %s.%s(", ir_method->method->parent->Decl().c_str(),
         ir_method->method->name->c_str());

  auto proto = ir_method->method->prototype;

  if (proto->param_types != nullptr) {
    bool first = true;
    for (auto type : proto->param_types->types) {
      printf("%s%s", (first ? "" : ", "), type->Decl().c_str());
      first = false;
    }
  }

  printf(") : %s\n{\n", proto->return_type->Decl().c_str());
  Dissasemble(ir_method);
  printf("}\n");
}

void DexDissasembler::Dissasemble(ir::EncodedMethod* ir_method) const {
  lir::CodeIr code_ir(ir_method, dex_ir_);
  PrintCodeIrVisitor visitor(dex_ir_);
  code_ir.Accept(&visitor);
}
