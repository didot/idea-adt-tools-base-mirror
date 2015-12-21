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
import com.android.sdklib.AndroidVersion;
import com.android.sdklib.IAndroidTarget;
import com.android.sdklib.ISystemImage;
import com.android.sdklib.repositoryv2.AndroidSdkHandler;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import junit.framework.TestCase;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * Tests for {@link AndroidTargetManager}.
 */
public class AndroidTargetManagerTest extends TestCase {

    public void testNew() throws Exception {
        MockFileOp fop = new MockFileOp();
        recordPlatform13(fop);
        recordPlatform23(fop);
        recordGoogleApisAddon23(fop);
        recordGoogleTvAddon13(fop);
        recordBuildTool23(fop);
        recordSysImg13(fop);
        recordGoogleApisSysImg23(fop);

        AndroidSdkHandler handler = new AndroidSdkHandler(fop);
        handler.setLocation(new File("/sdk"));
        FakeProgressIndicator progress = new FakeProgressIndicator();

        AndroidTargetManager mgr = handler.getAndroidTargetManager(progress);
        Collection<IAndroidTarget> targets = mgr.getTargets(progress);
        progress.assertNoErrorsOrWarnings();
        assertEquals(4, targets.size());
        Set<IAndroidTarget> sorted = Sets.newTreeSet(targets);
        Iterator<IAndroidTarget> iter = sorted.iterator();

        IAndroidTarget platform13 = iter.next();
        verifyPlatform13(platform13);
        verifyAddon13(iter.next(), platform13);
        IAndroidTarget platform23 = iter.next();
        verifyAddon23(iter.next(), platform23);
    }

    public void testMissing() throws Exception {
        MockFileOp fop = new MockFileOp();
        recordPlatform23(fop);
        recordGoogleTvAddon13(fop);
        recordBuildTool23(fop);
        recordSysImg13(fop);
        recordGoogleApisSysImg23(fop);

        AndroidSdkHandler handler = new AndroidSdkHandler(fop);
        handler.setLocation(new File("/sdk"));
        FakeProgressIndicator progress = new FakeProgressIndicator();

        AndroidTargetManager mgr = handler.getAndroidTargetManager(progress);
        Collection<IAndroidTarget> targets = mgr.getTargets(true, progress);
        progress.assertNoErrorsOrWarnings();
        // Now we get missing targets from all target-creating packages that don't have the
        // necessary dependencies to actually create a target.
        assertEquals(4, targets.size());

        Set<IAndroidTarget> sorted = Sets.newTreeSet(targets);
        Iterator<IAndroidTarget> iter = sorted.iterator();
        verifyMissing13(iter.next());
        verifyMissingAddon13(iter.next());
        verifyPlatform23(iter.next());
        verifyMissingAddon23(iter.next());
    }

    private static void verifyPlatform13(IAndroidTarget target) {
        assertEquals(new AndroidVersion(13, null), target.getVersion());
        assertEquals("Android Open Source Project", target.getVendor());
        assertEquals("/sdk/platforms/android-13/", target.getLocation());
        assertNull(target.getParent());
        ISystemImage[] images = target.getSystemImages();
        assertEquals(2, images.length);
        assertEquals(new File("/sdk/platforms/android-13/images"), images[0].getLocation());
        assertEquals(new File("/sdk/system-images/android-13/default/x86"),
                images[1].getLocation());
        assertEquals(ImmutableSet.of(new File("/sdk/platforms/android-13/skins/HVGA"),
                new File("/sdk/platforms/android-13/skins/WVGA800"),
                new File("/sdk/system-images/android-13/default/x86/skins/res1"),
                new File("/sdk/system-images/android-13/default/x86/skins/res2")),
                ImmutableSet.copyOf(target.getSkins()));
        assertEquals(ImmutableList.of("/sdk/platforms/android-13/android.jar"),
                target.getBootClasspath());
        assertEquals(new File("/sdk/build-tools/23.0.2"), target.getBuildToolInfo().getLocation());
        assertEquals(new File("/sdk/platforms/android-13/skins/WXGA"), target.getDefaultSkin());
    }

    private static void verifyMissing13(IAndroidTarget target) {
        assertEquals(new AndroidVersion(13, null), target.getVersion());
        assertNull(target.getVendor());
        assertNull(target.getLocation());
        assertNull(target.getParent());
        ISystemImage[] images = target.getSystemImages();
        assertEquals(1, images.length);
        assertEquals(new File("/sdk/system-images/android-13/default/x86"),
                images[0].getLocation());
        assertEquals(0, target.getSkins().length);
        assertTrue(target.getBootClasspath().isEmpty());
        assertNull(target.getBuildToolInfo());
        assertNull(target.getDefaultSkin());
    }

    private static void verifyPlatform23(IAndroidTarget target) {
        assertEquals(new AndroidVersion(23, null), target.getVersion());
        assertEquals("Android Open Source Project", target.getVendor());
        assertEquals("/sdk/platforms/android-23/", target.getLocation());
        assertNull(target.getParent());
        assertTrue(Arrays.deepEquals(new File[]{new File("/sdk/platforms/android-23/skins/HVGA"),
                        new File("/sdk/platforms/android-23/skins/WVGA800")},
                target.getSkins()));
        assertEquals(ImmutableList.of("/sdk/platforms/android-23/android.jar"),
                target.getBootClasspath());
        assertEquals(new File("/sdk/build-tools/23.0.2"), target.getBuildToolInfo().getLocation());
        assertEquals(new File("/sdk/platforms/android-23/skins/WVGA800"), target.getDefaultSkin());
        ISystemImage[] images = target.getSystemImages();
        assertEquals(0, images.length);
    }

    private static void verifyAddon13(IAndroidTarget target, IAndroidTarget platform13) {
        assertEquals(new AndroidVersion(13, null), target.getVersion());
        assertEquals("Google Inc.", target.getVendor());
        assertEquals("/sdk/add-ons/addon-google_tv_addon-google-13/", target.getLocation());
        assertEquals(platform13, target.getParent());
        assertEquals(ImmutableSet.of(
                new File("/sdk/platforms/android-13/skins/HVGA"),
                new File("/sdk/add-ons/addon-google_tv_addon-google-13/skins/1080p"),
                new File("/sdk/system-images/android-13/default/x86/skins/res2"),
                new File("/sdk/system-images/android-13/default/x86/skins/res1"),
                new File("/sdk/add-ons/addon-google_tv_addon-google-13/skins/720p-overscan"),
                new File("/sdk/platforms/android-13/skins/WVGA800")),
                ImmutableSet.copyOf(target.getSkins()));
        assertEquals(ImmutableList.of("/sdk/platforms/android-13/android.jar"),
                target.getBootClasspath());
        assertEquals(new File("/sdk/build-tools/23.0.2"), target.getBuildToolInfo().getLocation());
        assertEquals(new File("/sdk/add-ons/addon-google_tv_addon-google-13/skins/720p"),
                target.getDefaultSkin());
        ISystemImage[] images = target.getSystemImages();
        assertEquals(1, images.length);
        assertEquals(new File("/sdk/add-ons/addon-google_tv_addon-google-13/images/x86"),
                images[0].getLocation());
    }

    private static void verifyMissingAddon13(IAndroidTarget target) {
        assertEquals(new AndroidVersion(13, null), target.getVersion());
        assertEquals("Google Inc.", target.getVendor());
        assertNull(target.getLocation());
        assertNull(target.getParent());
        assertEquals(0, target.getSkins().length);
        assertTrue(target.getBootClasspath().isEmpty());
        assertNull(target.getBuildToolInfo());
        assertNull(target.getDefaultSkin());
        // The new implementation picks up the system image within the addon itself.
        ISystemImage[] images = target.getSystemImages();
        assertEquals(1, images.length);
        assertEquals(new File("/sdk/add-ons/addon-google_tv_addon-google-13/images/x86"),
                images[0].getLocation());
    }

    private static void verifyAddon23(IAndroidTarget target, IAndroidTarget platform23) {
        assertEquals(new AndroidVersion(23, null), target.getVersion());
        assertEquals("Google Inc.", target.getVendor());
        assertEquals("/sdk/add-ons/addon-google_apis-google-23/", target.getLocation());
        assertEquals(platform23, target.getParent());
        assertEquals(ImmutableSet.of(new File("/sdk/platforms/android-23/skins/HVGA"),
                new File("/sdk/platforms/android-23/skins/WVGA800")),
                ImmutableSet.copyOf(target.getSkins()));
        assertEquals(ImmutableList.of("/sdk/platforms/android-23/android.jar"),
                target.getBootClasspath());
        assertEquals(new File("/sdk/build-tools/23.0.2"), target.getBuildToolInfo().getLocation());
        assertEquals(new File("/sdk/platforms/android-23/skins/WVGA800"), target.getDefaultSkin());
        ISystemImage[] images = target.getSystemImages();
        assertEquals(1, images.length);
        assertEquals(new File("/sdk/system-images/android-23/google_apis/x86_64"),
                images[0].getLocation());
    }

    private static void verifyMissingAddon23(IAndroidTarget target) {
        assertEquals(new AndroidVersion(23, null), target.getVersion());
        assertEquals("Google Inc.", target.getVendor());
        assertNull(target.getLocation());
        assertNull(target.getParent());
        assertEquals(0, target.getSkins().length);
        assertTrue(target.getBootClasspath().isEmpty());
        assertNull(target.getBuildToolInfo());
        assertNull(target.getDefaultSkin());

        ISystemImage[] images = target.getSystemImages();
        assertEquals(1, images.length);
        assertEquals(new File("/sdk/system-images/android-23/google_apis/x86_64"),
                images[0].getLocation());
    }

    private static void recordBuildTool23(MockFileOp fop) {
        fop.recordExistingFile("/sdk/build-tools/23.0.2/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns2:sdk-repository "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-19E6313A\" type=\"text\">License text\n"
                        + "</license><localPackage path=\"build-tools;23.0.2\" obsolete=\"false\">"
                        + "<revision><major>23</major><minor>0</minor><micro>2</micro></revision>"
                        + "<display-name>Android SDK Build-Tools 23.0.2</display-name>"
                        + "<uses-license ref=\"license-19E6313A\"/></localPackage>"
                        + "</ns2:sdk-repository>\n");
    }

    private static void recordPlatform13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/platforms/android-13/images/system.img");
        fop.recordExistingFile("/sdk/platforms/android-13/android.jar");
        fop.recordExistingFile("/sdk/platforms/android-13/framework.aidl");
        fop.recordExistingFile("/sdk/platforms/android-13/skins/HVGA/layout");
        fop.recordExistingFile("/sdk/platforms/android-13/skins/dummy.txt");
        fop.recordExistingFile("/sdk/platforms/android-13/skins/WVGA800/layout");
        fop.recordExistingFile("/sdk/platforms/android-13/sdk.properties",
                "sdk.ant.templates.revision=1\n" +
                        "sdk.skin.default=WXGA\n");
        fop.recordExistingFile("/sdk/platforms/android-13/build.prop",
                "ro.build.id=HTJ85B\n"
                        + "ro.build.display.id=sdk-eng 3.2 HTJ85B 140714 test-keys\n"
                        + "ro.build.version.incremental=140714\n"
                        + "ro.build.version.sdk=13\n"
                        + "ro.build.version.codename=REL\n"
                        + "ro.build.version.release=3.2\n"
                        + "ro.build.date=Wed Jul  6 17:51:50 PDT 2011\n"
                        + "ro.build.date.utc=1309999910\n"
                        + "ro.build.type=eng\n"
                        + "ro.build.tags=test-keys\n"
                        + "ro.product.model=sdk\n"
                        + "ro.product.name=sdk\n"
                        + "ro.product.board=\n"
                        + "ro.product.cpu.abi=armeabi\n"
                        + "ro.product.locale.language=ldpi\n"
                        + "ro.wifi.channels=\n"
                        + "ro.board.platform=\n"
                        + "# ro.build.product is obsolete; use ro.product.device\n"
                        + "# Do not try to parse ro.build.description or .fingerprint\n"
                        + "ro.build.description=sdk-eng 3.2 HTJ85B 140714 test-keys\n"
                        + "ro.build.fingerprint=generic/sdk/generic:3.2/HTJ85B/140714:eng/test-keys\n"
                        + "ro.build.characteristics=default\n"
                        + "# end build properties\n"
                        + "#\n"
                        + "# system.prop for generic sdk \n"
                        + "#\n"
                        + "\n"
                        + "rild.libpath=/system/lib/libreference-ril.so\n"
                        + "rild.libargs=-d /dev/ttyS0\n"
                        + "\n"
                        + "#\n"
                        + "# ADDITIONAL_BUILD_PROPERTIES\n"
                        + "#\n"
                        + "ro.config.notification_sound=OnTheHunt.ogg\n"
                        + "ro.config.alarm_alert=Alarm_Classic.ogg\n"
                        + "ro.kernel.android.checkjni=1\n"
                        + "ro.setupwizard.mode=OPTIONAL\n"
                        + "xmpp.auto-presence=true\n"
                        + "ro.config.nocheckin=yes\n"
                        + "net.bt.name=Android\n"
                        + "dalvik.vm.stack-trace-file=/data/anr/traces.txt\n"
                        + "ro.build.user=generic\n"
                        + "ro.build.host=generic\n"
                        + "ro.product.brand=generic\n"
                        + "ro.product.manufacturer=generic\n"
                        + "ro.product.device=generic\n"
                        + "ro.build.product=generic\n");
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

    private static void recordPlatform23(MockFileOp fop) {
        fop.recordExistingFile("/sdk/platforms/android-23/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns2:sdk-repository "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-9A220565\" type=\"text\">Terms and Conditions\n"
                        + "</license><localPackage path=\"platforms;android-23\" "
                        + "obsolete=\"false\"><type-details "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns2:platformDetailsType\"><api-level>23</api-level>"
                        + "<layoutlib api=\"15\"/></type-details><revision><major>1</major>"
                        + "</revision><display-name>API 23: Android 6.0 (Marshmallow)"
                        + "</display-name><uses-license ref=\"license-9A220565\"/><dependencies>"
                        + "<dependency path=\"tools\"><min-revision><major>22</major>"
                        + "</min-revision></dependency></dependencies></localPackage>"
                        + "</ns2:sdk-repository>\n");
        fop.recordExistingFile("/sdk/platforms/android-23/android.jar");
        fop.recordExistingFile("/sdk/platforms/android-23/framework.aidl");
        fop.recordExistingFile("/sdk/platforms/android-23/skins/HVGA/layout");
        fop.recordExistingFile("/sdk/platforms/android-23/skins/dummy.txt");
        fop.recordExistingFile("/sdk/platforms/android-23/skins/WVGA800/layout");
        fop.recordExistingFile("/sdk/platforms/android-23/build.prop",
                "# autogenerated by buildinfo.sh\n"
                        + "ro.build.id=MRA44C\n"
                        + "ro.build.display.id=sdk_phone_armv7-eng 6.0 MRA44C 2166767 test-keys\n"
                        + "ro.build.version.incremental=2166767\n"
                        + "ro.build.version.sdk=23\n"
                        + "ro.build.version.preview_sdk=0\n"
                        + "ro.build.version.codename=REL\n"
                        + "ro.build.version.all_codenames=REL\n"
                        + "ro.build.version.release=6.0\n"
                        + "ro.build.version.security_patch=\n"
                        + "ro.build.version.base_os=\n"
                        + "ro.build.date=Thu Aug 13 23:46:41 UTC 2015\n"
                        + "ro.build.date.utc=1439509601\n"
                        + "ro.build.type=eng\n"
                        + "ro.build.tags=test-keys\n"
                        + "ro.build.flavor=sdk_phone_armv7-eng\n"
                        + "ro.product.model=sdk_phone_armv7\n"
                        + "ro.product.name=sdk_phone_armv7\n"
                        + "ro.product.board=\n"
                        + "# ro.product.cpu.abi and ro.product.cpu.abi2 are obsolete,\n"
                        + "# use ro.product.cpu.abilist instead.\n"
                        + "ro.product.cpu.abi=armeabi-v7a\n"
                        + "ro.product.cpu.abi2=armeabi\n"
                        + "ro.product.cpu.abilist=armeabi-v7a,armeabi\n"
                        + "ro.product.cpu.abilist32=armeabi-v7a,armeabi\n"
                        + "ro.product.cpu.abilist64=\n"
                        + "ro.product.locale=en-US\n"
                        + "ro.wifi.channels=\n"
                        + "ro.board.platform=\n"
                        + "# ro.build.product is obsolete; use ro.product.device\n"
                        + "# Do not try to parse description, fingerprint, or thumbprint\n"
                        + "ro.build.description=sdk_phone_armv7-eng 6.0 MRA44C 2166767 test-keys\n"
                        + "ro.build.fingerprint=generic/sdk_phone_armv7/generic:6.0/MRA44C/2166767:eng/test-keys\n"
                        + "ro.build.characteristics=default\n"
                        + "# end build properties\n"
                        + "#\n"
                        + "# from build/target/board/generic/system.prop\n"
                        + "#\n"
                        + "#\n"
                        + "# system.prop for generic sdk\n"
                        + "#\n"
                        + "\n"
                        + "rild.libpath=/system/lib/libreference-ril.so\n"
                        + "rild.libargs=-d /dev/ttyS0\n"
                        + "\n"
                        + "#\n"
                        + "# ADDITIONAL_BUILD_PROPERTIES\n"
                        + "#\n"
                        + "ro.config.notification_sound=OnTheHunt.ogg\n"
                        + "ro.config.alarm_alert=Alarm_Classic.ogg\n"
                        + "persist.sys.dalvik.vm.lib.2=libart\n"
                        + "dalvik.vm.isa.arm.variant=generic\n"
                        + "dalvik.vm.isa.arm.features=default\n"
                        + "ro.kernel.android.checkjni=1\n"
                        + "dalvik.vm.lockprof.threshold=500\n"
                        + "dalvik.vm.usejit=true\n"
                        + "xmpp.auto-presence=true\n"
                        + "ro.config.nocheckin=yes\n"
                        + "net.bt.name=Android\n"
                        + "dalvik.vm.stack-trace-file=/data/anr/traces.txt\n"
                        + "ro.build.user=generic\n"
                        + "ro.build.host=generic\n"
                        + "ro.product.brand=generic\n"
                        + "ro.product.manufacturer=generic\n"
                        + "ro.product.device=generic\n"
                        + "ro.build.product=generic\n");
    }

    private static void recordGoogleApisAddon23(MockFileOp fop) {
        fop.recordExistingFile("/sdk/add-ons/addon-google_apis-google-23/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns5:sdk-addon "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-1E15FA4A\" type=\"text\">Terms and Conditions\n"
                        + "</license><localPackage path=\"add-ons;addon-google_apis-google-23-1\" "
                        + "obsolete=\"false\"><type-details "
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns5:addonDetailsType\"><api-level>23</api-level><vendor>"
                        + "<id>google</id><display>Google Inc.</display></vendor><tag>"
                        + "<id>google_apis</id><display>Google APIs</display></tag></type-details>"
                        + "<revision><major>1</major><minor>0</minor><micro>0</micro></revision>"
                        + "<display-name>Google APIs, Android 23</display-name><uses-license "
                        + "ref=\"license-1E15FA4A\"/></localPackage></ns5:sdk-addon>\n");
    }

    private static void recordGoogleTvAddon13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                        "<ns5:sdk-addon xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        +
                        "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" " +
                        "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" " +
                        "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">" +
                        "<license id=\"license-A06C75BE\" type=\"text\">Terms and Conditions\n" +
                        "</license><localPackage " +
                        "path=\"add-ons;addon-google_tv_addon-google-13\" obsolete=\"false\">" +
                        "<type-details xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                        "xsi:type=\"ns5:addonDetailsType\"><api-level>13</api-level>" +
                        "<vendor><id>google</id><display>Google Inc.</display></vendor>" +
                        "<tag><id>google_tv_addon</id><display>Google TV Addon</display></tag>" +
                        "<default-skin>720p</default-skin>" +
                        "</type-details><revision><major>1</major><minor>0</minor>" +
                        "<micro>0</micro></revision>" +
                        "<display-name>Google TV Addon, Android 13</display-name>" +
                        "<uses-license ref=\"license-A06C75BE\"/></localPackage>" +
                        "</ns5:sdk-addon>\n");
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/skins/1080p/layout");
        fop.recordExistingFile("/sdk/add-ons/addon-google_tv_addon-google-13/skins/dummy.txt");
        fop.recordExistingFile(
                "/sdk/add-ons/addon-google_tv_addon-google-13/skins/720p-overscan/layout");
        fop.recordExistingFile(
                "/sdk/add-ons/addon-google_tv_addon-google-13/images/x86/system.img");
    }

    private static void recordSysImg13(MockFileOp fop) {
        fop.recordExistingFile("/sdk/system-images/android-13/default/x86/system.img");
        fop.recordExistingFile("/sdk/system-images/android-13/default/x86/skins/res1/layout");
        fop.recordExistingFile("/sdk/system-images/android-13/default/x86/skins/dummy");
        fop.recordExistingFile("/sdk/system-images/android-13/default/x86/skins/res2/layout");
        fop.recordExistingFile("/sdk/system-images/android-13/default/x86/package.xml",
                "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                        + "<ns3:sdk-sys-img "
                        + "xmlns:ns2=\"http://schemas.android.com/sdk/android/repo/repository2/01\" "
                        + "xmlns:ns3=\"http://schemas.android.com/sdk/android/repo/sys-img2/01\" "
                        + "xmlns:ns4=\"http://schemas.android.com/repository/android/common/01\" "
                        + "xmlns:ns5=\"http://schemas.android.com/sdk/android/repo/addon2/01\">"
                        + "<license id=\"license-A78C4257\" type=\"text\">Terms and Conditions\n"
                        + "</license><localPackage path=\"system-images;android-13;default;x86\" "
                        + "obsolete=\"false\">"
                        + "<type-details xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "xsi:type=\"ns3:sysImgDetailsType\"><api-level>13</api-level>"
                        + "<tag><id>default</id><display>Default</display></tag><abi>x86</abi>"
                        + "</type-details><revision><major>5</major></revision>"
                        + "<display-name>Intel x86 Atom System Image</display-name>"
                        + "<uses-license ref=\"license-A78C4257\"/></localPackage>"
                        + "</ns3:sdk-sys-img>\n");
    }

    private static void recordGoogleApisSysImg23(MockFileOp fop) {
        fop.recordExistingFile("/sdk/system-images/android-23/google_apis/x86_64/system.img");
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

}
