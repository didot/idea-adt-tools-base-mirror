<preference-headers xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- These settings headers are only used on tablets. -->

    <header
        android:fragment="${packageName}.${activityClass}$GeneralPreferenceFragment"
        android:title="@string/pref_header_general" />

    <header
        android:fragment="${packageName}.${activityClass}$NotificationPreferenceFragment"
        android:title="@string/pref_header_notifications" />

    <header
        android:fragment="${packageName}.${activityClass}$DataSyncPreferenceFragment"
        android:title="@string/pref_header_data_sync" />

</preference-headers>
