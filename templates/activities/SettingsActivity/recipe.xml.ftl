<?xml version="1.0"?>
<#import "root://activities/common/kotlin_macros.ftl" as kt>
<recipe>
    <@kt.addAllKotlinDependencies />
    <dependency mavenUrl="com.android.support:support-v4:${buildApi}.+" />
<#if minApiLevel lt 21>
    <dependency mavenUrl="com.android.support:support-vector-drawable:${buildApi}.+" />

    <merge from="root/build.gradle"
             to="${escapeXmlAttribute(projectOut)}/build.gradle" />
</#if>

    <#include "../common/recipe_manifest.xml.ftl" />

    <copy from="root/res/xml/pref_data_sync.xml"
            to="${escapeXmlAttribute(resOut)}/xml/pref_data_sync.xml" />
    <copy from="root/res/xml/pref_general.xml"
            to="${escapeXmlAttribute(resOut)}/xml/pref_general.xml" />
    <copy from="root/res/xml/pref_notification.xml"
            to="${escapeXmlAttribute(resOut)}/xml/pref_notification.xml" />

    <instantiate from="root/res/xml/pref_headers.xml.ftl"
                   to="${escapeXmlAttribute(resOut)}/xml/pref_headers.xml" />

    <copy from="root/res/drawable"
            to="${escapeXmlAttribute(resOut)}/drawable" />

    <merge from="root/res/values/pref_strings.xml.ftl"
             to="${escapeXmlAttribute(resOut)}/values/strings.xml" />

<#if generateKotlin>
    <instantiate from="root/src/app_package/SettingsActivity.kt.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.kt" />
    <#if appCompatActivity>
        <instantiate from="root/src/app_package/AppCompatPreferenceActivity.kt.ftl"
                       to="${escapeXmlAttribute(srcOut)}/AppCompatPreferenceActivity.kt" />
    </#if>
    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.kt" />
<#else>
    <instantiate from="root/src/app_package/SettingsActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
    <#if appCompatActivity>
         <instantiate from="root/src/app_package/AppCompatPreferenceActivity.java.ftl"
                   to="${escapeXmlAttribute(srcOut)}/AppCompatPreferenceActivity.java" />
    </#if>
    <open file="${escapeXmlAttribute(srcOut)}/${activityClass}.java" />
</#if>
</recipe>
