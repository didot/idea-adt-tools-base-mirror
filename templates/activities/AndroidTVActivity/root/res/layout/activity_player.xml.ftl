<?xml version="1.0" encoding="utf-8"?>
<RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/container"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:deviceIds="tv">

    <VideoView
        android:id="@+id/videoView"
        android:layout_width="fill_parent"
        android:layout_height="fill_parent"
        android:layout_alignParentBottom="true"
        android:layout_alignParentLeft="true"
        android:layout_alignParentRight="true"
        android:layout_alignParentTop="true"
        android:layout_centerInParent="true"
        android:layout_gravity="center" />

    <RelativeLayout
        android:id="@+id/controllers"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_alignBottom="@+id/videoView"
        android:layout_alignLeft="@+id/videoView"
        android:layout_alignRight="@+id/videoView"
        android:layout_alignTop="@+id/videoView"
        android:layout_centerInParent="true"
        android:background="@drawable/player_bg_gradient_dark">

        <ProgressBar
            android:id="@+id/progressBar"
            style="@android:style/Widget.ProgressBar.Horizontal"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_centerInParent="true"
            android:visibility="gone" />

        <RelativeLayout
            android:layout_width="fill_parent"
            android:layout_height="45dp"
            android:layout_alignParentBottom="true">

            <ImageView
                android:id="@+id/playpause"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_alignParentLeft="true"
                android:contentDescription="@+id/play_pause_description"
                android:src="@drawable/ic_play_playcontrol_normal" />

            <TextView
                android:id="@+id/startText"
                android:layout_width="wrap_content"
                android:layout_height="fill_parent"
                android:layout_marginLeft="5dp"
                android:layout_toRightOf="@+id/playpause"
                android:gravity="center_vertical"
                android:maxLines="1"
                android:text="@+id/init_text"
                android:textColor="@color/white" />

            <TextView
                android:id="@+id/endText"
                android:layout_width="wrap_content"
                android:layout_height="fill_parent"
                android:layout_alignParentRight="true"
                android:layout_marginRight="16dp"
                android:gravity="center_vertical"
                android:maxLines="1"
                android:text="@+id/init_text"
                android:textColor="@color/white" />

            <SeekBar
                android:id="@+id/seekBar"
                android:layout_width="fill_parent"
                android:layout_height="wrap_content"
                android:layout_centerVertical="true"
                android:layout_gravity="center"
                android:layout_marginLeft="5dp"
                android:layout_marginRight="5dp"
                android:layout_toLeftOf="@+id/endText"
                android:layout_toRightOf="@+id/startText" />
        </RelativeLayout>
    </RelativeLayout>
</RelativeLayout>
