<?xml version="1.0" encoding="utf-8"?>
<android.support.design.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/main_content"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:fitsSystemWindows="true"
    tools:context="${packageName}.${activityClass}">

    <android.support.design.widget.AppBarLayout
        android:id="@+id/appbar"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:paddingTop="@dimen/appbar_padding_top"
        android:theme="@style/${themeNameAppBarOverlay}">

        <android.support.v7.widget.Toolbar
            android:id="@+id/toolbar"
            app:title="@string/app_name"
            android:layout_width="match_parent"
            android:layout_height="?attr/actionBarSize"
            android:layout_weight="1"
            android:background="?attr/colorPrimary"
            app:popupTheme="@style/${themeNamePopupOverlay}"
            app:layout_scrollFlags="scroll|enterAlways">

            <#if features == 'spinner'>
                <Spinner
                    android:id="@+id/spinner"
                    app:popupTheme="@style/${themeNamePopupOverlay}"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content" />
            </#if>
        </android.support.v7.widget.Toolbar>

        <#if features == 'tabs'>
        <android.support.design.widget.TabLayout
            android:id="@+id/tabs"
            android:layout_width="match_parent"
            android:layout_height="wrap_content">

            <android.support.design.widget.TabItem
                android:id="@+id/tabItem"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/tab_text_1" />

            <android.support.design.widget.TabItem
                android:id="@+id/tabItem2"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/tab_text_2" />

            <android.support.design.widget.TabItem
                android:id="@+id/tabItem3"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:text="@string/tab_text_3" />

        </android.support.design.widget.TabLayout>
        </#if>
    </android.support.design.widget.AppBarLayout>

    <${viewContainer}
        android:id="@+id/container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout_behavior="@string/appbar_scrolling_view_behavior"/>

    <android.support.design.widget.FloatingActionButton
        android:id="@+id/fab"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="end|bottom"
        android:layout_margin="@dimen/fab_margin"
        app:srcCompat="@android:drawable/ic_dialog_email" />

</android.support.design.widget.CoordinatorLayout>
