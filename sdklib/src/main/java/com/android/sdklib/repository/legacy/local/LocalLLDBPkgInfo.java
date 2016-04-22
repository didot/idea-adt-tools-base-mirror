/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.sdklib.repository.legacy.local;

import com.android.annotations.NonNull;
import com.android.repository.api.RepoManager;
import com.android.sdklib.repository.AndroidSdkHandler;
import com.android.sdklib.repository.legacy.descriptors.IPkgDesc;
import com.android.sdklib.repository.legacy.descriptors.PkgDesc;
import com.android.repository.Revision;

import java.io.File;
import java.util.Properties;

/**
 * Local package representing the Android LLDB.
 *
 * @deprecated This is part of the old SDK manager framework. Use
 * {@link AndroidSdkHandler}/{@link RepoManager} and associated classes instead.
 */
@Deprecated
class LocalLLDBPkgInfo extends LocalPkgInfo {
  /**
   * The LLDB SDK package revision's major and minor numbers are pinned in Android Studio.
   *
   * @deprecated in favor of LLDBSdkPackageInstaller#PINNED_REVISION.
   */

  @Deprecated
  static final Revision PINNED_REVISION = new Revision(2, 0);

  @NonNull
  private final IPkgDesc mDesc;

  LocalLLDBPkgInfo(@NonNull LocalSdk localSdk,
          @NonNull File localDir,
          @NonNull Properties sourceProps,
          @NonNull Revision revision) {
    super(localSdk, localDir, sourceProps);
    mDesc = PkgDesc.Builder.newLLDB(revision).setDescriptionShort("LLDB").setListDisplay("LLDB").create();
  }

  @NonNull
  @Override
  public IPkgDesc getDesc() {
    return mDesc;
  }
}
