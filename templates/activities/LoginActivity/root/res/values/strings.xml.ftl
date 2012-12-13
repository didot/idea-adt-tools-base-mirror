<resources>
    <#if !isNewProject>
    <string name="title_${simpleName}">${escapeXmlString(activityTitle)}</string>
    </#if>

    <!-- Strings related to login -->
    <string name="prompt_email">Email</string>
    <string name="prompt_password">Password</string>

    <string name="action_sign_in_register"><b>Sign in</b> or register</string>
    <string name="action_sign_in_short">Sign in</string>

    <string name="menu_forgot_password">Recover lost password</string>

    <string name="login_progress_signing_in">Signing in&#8230;</string>

    <string name="error_invalid_email">This email address is invalid</string>
    <string name="error_invalid_password">This password is too short</string>
    <string name="error_incorrect_password">This password is incorrect</string>
    <string name="error_field_required">This field is required</string>
</resources>
