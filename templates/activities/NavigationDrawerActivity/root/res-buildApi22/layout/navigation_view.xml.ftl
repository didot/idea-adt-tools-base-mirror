<?xml version="1.0" encoding="utf-8"?>
<${getMaterialComponentName('android.support.v4.widget.DrawerLayout', useAndroidX)}
    xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/drawer_layout"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    tools:openDrawer="start">

    <include
        layout="@layout/${appBarLayoutName}"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

    <${getMaterialComponentName('android.support.design.widget.NavigationView', useMaterial2)}
        android:id="@+id/nav_view"
        android:layout_width="wrap_content"
        android:layout_height="match_parent"
        android:layout_gravity="start"
        android:fitsSystemWindows="true"
        app:headerLayout="@layout/${navHeaderLayoutName}"
        app:menu="@menu/${drawerMenu}" />

</${getMaterialComponentName('android.support.v4.widget.DrawerLayout', useAndroidX)}>
