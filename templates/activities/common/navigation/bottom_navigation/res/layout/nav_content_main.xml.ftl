<?xml version="1.0" encoding="utf-8"?>
<layout xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto">

    <${getMaterialComponentName('android.support.constraint.ConstraintLayout', useMaterial2)}
        android:id="@+id/container"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:paddingTop="?attr/actionBarSize" >

        <${getMaterialComponentName('android.support.design.widget.BottomNavigationView', useMaterial2)}
            android:id="@+id/nav_view"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginStart="0dp"
            android:layout_marginEnd="0dp"
            android:background="?android:attr/windowBackground"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:menu="@menu/bottom_nav_menu"/>

        <fragment
            android:id="@+id/nav_host_fragment"
            android:name="androidx.navigation.fragment.NavHostFragment"
            android:layout_width="match_parent"
            android:layout_height="match_parent"
            app:defaultNavHost="true"
            app:layout_constraintBottom_toTopOf="@id/nav_view"
            app:layout_constraintLeft_toLeftOf="parent"
            app:layout_constraintRight_toRightOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            app:navGraph="@navigation/mobile_navigation"
        />

    </${getMaterialComponentName('android.support.constraint.ConstraintLayout', useMaterial2)}>
</layout>