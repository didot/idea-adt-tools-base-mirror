/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.sdklib.repositoryv2.targets;

import com.android.repository.testframework.FakeProgressIndicator;
import com.android.repository.testframework.MockFileOp;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.repository.local.LocalSdk;
import com.android.sdklib.repositoryv2.AndroidSdkHandler;
import com.android.sdklib.repositoryv2.meta.SysImgFactory;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Tests for {@link SystemImageManager}
 */
public class SystemImageManagerTest extends TestCase {
    // TODO: break up tests into separate cases

    public void testLegacy() throws Exception {
        MockFileOp fop = new MockFileOp();
        recordLegacyPlatform13(fop);
        recordLegacyGoogleTvAddon13(fop);
        recordLegacyGoogleApisSysImg23(fop);
        recordLegacySysImg23(fop);
        recordLegacyGoogleApis13(fop);
        LocalSdk sdk = new LocalSdk(fop);
        sdk.setLocation(new File("/sdk"));

        SystemImageManager mgr = new SystemImageManager(sdk);
        FakeProgressIndicator progress = new FakeProgressIndicator();
        Set<ISystemImage> targets = Sets.newTreeSet(mgr.getImages(progress));

        progress.assertNoErrorsOrWarnings();
        assertEquals(5, targets.size());
        Iterator<ISystemImage> resultIter = targets.iterator();
        verifyGoogleAddon13(resultIter.next());
        verifyPlatform13(resultIter.next());
        verifySysImg23(resultIter.next());
        verifyTvAddon13(resultIter.next());
        verifyGoogleApisSysImg23(resultIter.next());
    }

    public void testNew() throws Exception {
        MockFileOp fop = new MockFileOp();
        recordNewPlatform13(fop);
        recordNewGoogleTvAddon13(fop);
        recordNewGoogleApisSysImg23(fop);
        recordNewSysImg23(fop);
        recordNewGoogleApis13(fop);

        AndroidSdkHandler handler = new AndroidSdkHandler(fop, true);
        handler.setLocation(new File("/sdk"));
        FakeProgressIndicator progress = new FakeProgressIndicator();

        SystemImageManager mgr = new SystemImageManager(handler.getSdkManager(progress),
                (SysImgFactory)handler.getSysImgModule(progress).createLatestFactory(), fop);
        Set<ISystemImage> images = Sets.newTreeSet(mgr.getImages(progress));
        progress.assertNoErrorsOrWarnings();
        assertEquals(5, images.size());
        Iterator<ISystemImage> resultIter = images.iterator();

        ISystemImage platform13 = resultIter.next();
        verifyPlatform13(platform13);
        assertEquals(2, platform13.getSkins().length);

        verifySysImg23(resultIter.next());

        ISystemImage google13 = resultIter.next();
        verifyGoogleAddon13(google13);
        assertEquals(2, google13.getSkins().length);

        ISystemImage google23 = resultIter.next();
        verifyGoogleApisSysImg23(google23);

        ISystemImage addon13 = resultIter.next();
        verifyTvAddon13(addon13);
        assertEquals("google_tv_addon", addon13.getTag().getId());
    }

    private void verifyGoogleAddon13(ISystemImage img) {
        // Nothing, just here for consistency. Note the new implementation will pick up skins from
        // the platform.
    }

    private static void verifyPlatform13(ISystemImage img) {
        assertEquals("armeabi", img.getAbiType());
        assertNull(img.getAddonVendor());
        assertEquals(new File("/sdk/platforms/android-13/images/"), img.getLocation());
        assertEquals("default", img.getTag().getId());
    }

    private static void verifyTvAddon13(ISystemImage img) {
        assertEquals("x86", img.getAbiType());
        assertEquals("google", img.getAddonVendor().getId());
        assertEquals(new File("/sdk/add-ons/addon-google_tv_addon-google-13/images/x86/"),
                img.getLocation());
    }

    private static void verifyGoogleApisSysImg23(ISystemImage img) {
        assertEquals("x86_64", img.getAbiType());
        assertEquals("google", img.getAddonVendor().getId());
        assertEquals(new File("/sdk/system-images/android-23/google_apis/x86_64/"),
                img.getLocation());
        assertEquals("google_apis", img.getTag().getId());
    }

    private static void verifySysImg23(ISystemImage img) {
        assertEquals("x86", img.getAbiType());
        assertNull(img.getAddonVendor());
        assertEquals(new File("/sdk/system-images/android-23/default/x86/"),
                img.getLocation());
        assertEquals(2, img.getSkins().length);
        assertTrue(Arrays.equals(new File[] {new File("/sdk/system-images/android-23/default/x86/skins/res1/"),
                new File("/sdk/system-images/android-23/default/x86/skins/res2/")}, img.getSkins()));
        assertEquals("default", img.getTag().getId());
    }

    private static void recordLegacyPlatform13(MockFileOp fop) {
        recordCommonPlatform13(fop);
        fop.recordExistingFile("/sdk/platforms/android-13/source.properties",
                "AndroidVersion.ApiLevel=13\n"
                        + "Layoutlib.Api=4\n"
                        + "Layoutlib.Revision=0\n"
                        + "Pkg.Desc=Android SDK Platform 3.2, revision 1\n"
                        + "Pkg.DescUrl=http\\://developer.android.com/sdk/\n"
                        + "Pkg.LicenseRef=android-sdk-license\n"
                        + "Pkg.Obsolete=\n"
                        + "Pkg.Revision=1\n"
                        + "Pkg.SourceUrl=https\\://dl.google.com/android/repository/repository-11.xml\n"
                        + "Platform.MinToolsRev=12\n"
                        + "Platform.Version=3.2\n");
        fop.recordExistingFile("/sdk/platforms/android-13/build.prop",
                "ro.build.version.sdk=13\n");
    }

    private static void recordNewPlatform13(MockFileOp fop) {
        recordCommonPlatform13(fop);
        fop.recordExistingFile("/sdk/platforms/android-13/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns2:sdk-repository "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-2A86BE32\" type=\"text\">License Text\n</license>"
                        + "<localPackage path=\"platforms;android-13\" obsolete=\"false\">"
                        + "<type-details xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns2:platformDetailsType\"><api-level>13</api-level>"
                        + "<layoutlib api=\"4\"/></type-details><revision><major>1</major>"
                        + "</revision><display-name>API 13: Android 3.2 (Honeycomb)</display-name>"
                        + "<uses-license ref=\"license-2A86BE32\"/><dependencies>"
                        + "<dependency path=\"tools\"><min-revision><major>12</major></min-revision>"
                        + "</dependency></dependencies></localPackage></ns2:sdk-repository>");
    }

    private static void recordCommonPlatform13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/platforms/android-13/images/system.img");

        fop.recordExistingFile("/sdk/platforms/android-13/android.jar");
        fop.recordExistingFile("/sdk/platforms/android-13/framework.aidl");
        fop.recordExistingFile("/sdk/platforms/android-13/skins/HVGA/layout");
        fop.recordExistingFile("/sdk/platforms/android-13/skins/dummy.txt");
        fop.recordExistingFile("/sdk/platforms/android-13/skins/WVGA800/layout");
        fop.recordExistingFile("/sdk/platforms/android-13/sdk.properties",
                "sdk.ant.templates.revision=1\n" +
                        "sdk.skin.default=WXGA\n");
    }


    private static void recordLegacyGoogleTvAddon13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/source.properties",
                "Addon.NameDisplay=Google TV Addon\n"
                        + "Addon.NameId=google_tv_addon\n"
                        + "Addon.VendorDisplay=Google Inc.\n"
                        + "Addon.VendorId=google\n"
                        + "AndroidVersion.ApiLevel=13\n"
                        + "Pkg.Desc=Android + Google TV, API 13\n"
                        + "Pkg.DescUrl=http\\://developer.android.com/\n"
                        + "Pkg.LicenseRef=android-googletv-license\n"
                        + "Pkg.Obsolete=\n"
                        + "Pkg.Revision=1\n"
                        + "Pkg.SourceUrl=https\\://dl.google.com/android/repository/addon.xml\n");
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/manifest.ini",
                "name=Google TV Addon\n"
                        + "vendor=Google Inc.\n"
                        + "\n"
                        + "# version of the Android platform on which this add-on is built.\n"
                        + "api=13\n"
                        + "\n"
                        + "# revision of the add-on\n"
                        + "revision=1\n"
                        + "\n"
                        + "# default skin name\n"
                        + "skin=720p\n");
        recordCommonGoogleTvAddon13(fop);
    }

    private static void recordCommonGoogleTvAddon13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/skins/1080p/layout");
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/skins/dummy.txt");
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/skins/720p-overscan/layout");
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/images/x86/system.img");
    }

    private static void recordNewGoogleTvAddon13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns5:sdk-addon xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-A06C75BE\" type=\"text\">Terms and Conditions\n"
                        + "</license><localPackage "
                        + "path=\"add-ons;addon-google_tv_addon-google-13\" obsolete=\"false\">"
                        + "<type-details xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns5:addonDetailsType\"><api-level>13</api-level>"
                        + "<vendor><id>google</id><display>Google Inc.</display></vendor>"
                        + "<tag><id>google_tv_addon</id><display>Google TV Addon</display></tag>"
                        + "<default-skin>720p</default-skin>"
                        + "</type-details><revision><major>1</major><minor>0</minor>"
                        + "<micro>0</micro></revision>"
                        + "<display-name>Google TV Addon, Android 13</display-name>"
                        + "<uses-license ref=\"license-A06C75BE\"/></localPackage>"
                        + "</ns5:sdk-addon>\n");
        recordCommonGoogleTvAddon13(fop);
    }

    private static void recordLegacySysImg23(MockFileOp fop) {
        recordCommonSysImg23(fop);
        fop.recordExistingFile("/sdk/system-images/android-23/default/x86/source.properties",
                "AndroidVersion.ApiLevel=23\n"
                        + "Pkg.Desc=Android SDK Platform 6.0\n"
                        + "Pkg.LicenseRef=android-sdk-license\n"
                        + "Pkg.Revision=5\n"
                        + "Pkg.SourceUrl=https\\://dl.google.com/android/repository/sys-img/android/sys-img.xml\n"
                        + "SystemImage.Abi=x86\n"
                        + "SystemImage.TagDisplay=Default\n"
                        + "SystemImage.TagId=default\n");
    }

    private static void recordNewSysImg23(MockFileOp fop) {
        recordCommonSysImg23(fop);
        fop.recordExistingFile("/sdk/system-images/android-23/default/x86/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns3:sdk-sys-img "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-A78C4257\" type=\"text\">Terms and Conditions\n"
                        + "</license><localPackage path=\"system-images;android-23;default;x86\" "
                        + "obsolete=\"false\">"
                        + "<type-details xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns3:sysImgDetailsType\"><api-level>23</api-level>"
                        + "<tag><id>default</id><display>Default</display></tag><abi>x86</abi>"
                        + "</type-details><revision><major>5</major></revision>"
                        + "<display-name>Intel x86 Atom System Image</display-name>"
                        + "<uses-license ref=\"license-A78C4257\"/></localPackage>"
                        + "</ns3:sdk-sys-img>\n");
    }

    private static void recordCommonSysImg23(MockFileOp fop) {
        fop.recordExistingFile("/sdk/system-images/android-23/default/x86/system.img");
        fop.recordExistingFile("/sdk/system-images/android-23/default/x86/skins/res1/layout");
        fop.recordExistingFile("/sdk/system-images/android-23/default/x86/skins/dummy");
        fop.recordExistingFile("/sdk/system-images/android-23/default/x86/skins/res2/layout");
    }

    private static void recordLegacyGoogleApisSysImg23(MockFileOp fop) {
        recordCommonGoogleApisSysImg23(fop);
        fop.recordExistingFile("/sdk/system-images/android-23/google_apis/x86_64/source.properties",
                "Addon.VendorDisplay=Google Inc.\n"
                        + "Addon.VendorId=google\n"
                        + "AndroidVersion.ApiLevel=23\n"
                        + "Pkg.Desc=System Image x86_64 with Google APIs.\n"
                        + "Pkg.LicenseRef=android-sdk-license\n"
                        + "Pkg.Revision=9\n"
                        + "Pkg.SourceUrl=https\\://dl.google.com/android/repository/sys-img/google_apis/sys-img.xml\n"
                        + "SystemImage.Abi=x86_64\n"
                        + "SystemImage.TagDisplay=Google APIs\n"
                        + "SystemImage.TagId=google_apis\n");
    }

    private static void recordNewGoogleApisSysImg23(MockFileOp fop) {
        recordCommonGoogleApisSysImg23(fop);
        fop.recordExistingFile("/sdk/system-images/android-23/google_apis/x86_64/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns3:sdk-sys-img "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-9A5C00D5\" type=\"text\">Terms and Conditions\n"
                        + "</license><localPackage "
                        + "path=\"system-images;android-23;google_apis;x86_64\" "
                        + "obsolete=\"false\"><type-details "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns3:sysImgDetailsType\"><api-level>23</api-level>"
                        + "<tag><id>google_apis</id><display>Google APIs</display></tag>"
                        + "<vendor><id>google</id><display>Google Inc.</display></vendor>"
                        + "<abi>x86_64</abi></type-details><revision><major>9</major></revision>"
                        + "<display-name>Google APIs Intel x86 Atom_64 System Image</display-name>"
                        + "<uses-license ref=\"license-9A5C00D5\"/></localPackage>"
                        + "</ns3:sdk-sys-img>\n");
    }

    private static void recordCommonGoogleApisSysImg23(MockFileOp fop) {
        fop.recordExistingFile("/sdk/system-images/android-23/google_apis/x86_64/system.img");
    }

    private static void recordLegacyGoogleApis13(MockFileOp fop) {
        recordCommonGoogleApis13(fop);
        fop.recordExistingFile("/sdk/add-ons/addon-google_apis-google-13/source.properties",
                "Addon.NameDisplay=Google APIs\n"
                        + "Addon.NameId=google_apis\n"
                        + "Addon.VendorDisplay=Google Inc.\n"
                        + "Addon.VendorId=google\n"
                        + "AndroidVersion.ApiLevel=13\n"
                        + "Pkg.Desc=Android + Google APIs, API 13, revision 1\n"
                        + "Pkg.DescUrl=http\\://developer.android.com/\n"
                        + "Pkg.Obsolete=\n"
                        + "Pkg.Revision=1\n"
                        + "Pkg.SourceUrl=https\\://dl.google.com/android/repository/addon.xml\n");
        fop.recordExistingFile("/sdk/add-ons/addon-google_apis-google-13/manifest.ini",
                "name=Google APIs\n"
                        + "vendor=Google Inc.\n"
                        + "description=Android + Google APIs\n"
                        + "api=13\n"
                        + "revision=1\n");
    }

    private static void recordNewGoogleApis13(MockFileOp fop) {
        recordCommonGoogleApis13(fop);
        fop.recordExistingFile("/sdk/add-ons/addon-google_apis-google-13/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                        + "<ns5:sdk-addon "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">\n"
                        + "<license id=\"license-DB79309F\" type=\"text\">\n"
                        + "Terms and Conditions\n"
                        + "</license>\n"
                        + "<localPackage path=\"add-ons;addon-google_apis-google-13\" "
                        + "obsolete=\"false\">\n"
                        + "<type-details xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns5:addonDetailsType\">\n"
                        + "<api-level>13</api-level>\n"
                        + "<vendor>\n"
                        + "<id>google</id>\n"
                        + "<display>Google Inc.</display>\n"
                        + "</vendor>\n"
                        + "<tag>\n"
                        + "<id>google_apis</id>\n"
                        + "<display>\n"
                        + "Google APIs</display>\n"
                        + "</tag>\n"
                        + "</type-details>\n"
                        + "<revision>\n"
                        + "<major>1</major>\n"
                        + "<minor>0</minor>\n"
                        + "<micro>0</micro>\n"
                        + "</revision>\n"
                        + "<display-name>Google APIs, Android 13</display-name>\n"
                        + "<uses-license ref=\"license-DB79309F\"/>\n"
                        + "</localPackage>\n"
                        + "</ns5:sdk-addon>\n");
    }

    private static void recordCommonGoogleApis13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/add-ons/addon-google_apis-google-13/images/system.img");
    }
}
