<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#0099cc"
    tools:context="${packageName}.${activityClass}">

    <!-- The primary full-screen view. This can be replaced with whatever view
         is needed to present your content, e.g. VideoView, SurfaceView,
         TextureView, etc. -->
    <TextView android:id="@+id/fullscreen_content"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:keepScreenOn="true"
        android:textColor="#33b5e5"
        android:textStyle="bold"
        android:textSize="50sp"
        android:gravity="center"
        android:text="@string/dummy_content" />

    <!-- This FrameLayout insets its children based on system windows using
         android:fitsSystemWindows. -->
    <FrameLayout android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:fitsSystemWindows="true">

        <LinearLayout android:id="@+id/fullscreen_content_controls"
            style="?metaButtonBarStyle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_gravity="bottom|center_horizontal"
            android:background="@color/black_overlay"
            android:orientation="horizontal"
            tools:ignore="UselessParent">

            <Button android:id="@+id/dummy_button"
                style="?metaButtonBarButtonStyle"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:text="@string/dummy_button" />

        </LinearLayout>
    </FrameLayout>

</FrameLayout>
