package com.android.tests.shrink;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

public class RootActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.used1);
        ResourceReferences.referenceResources(this);
        System.out.println(R.layout.used7);
        AnnotationInflation.createView(this, ScreenType1.class, null);
        AnnotationInflation.createView(this, ScreenType2.class, null);

        for (int id : layout_ids) {
            System.out.println(id);
        }
    }

    public void unusedMethod() {
        Drawable drawable = getResources().getDrawable(R.drawable.unused10);
        System.out.println(drawable);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.used13, menu);
        return true;
    }


    @Layout(R.layout.used17)
    @Retention(RetentionPolicy.RUNTIME)
    @Target({METHOD, PARAMETER, TYPE, LOCAL_VARIABLE, FIELD})
    public @interface Indirect {
        int[] value();
    }

    @Layout(R.layout.used16)
    private static class ScreenType1 {
    }

    @Indirect(5)
    @Layouts({R.layout.used18,R.layout.used19})
    private static class ScreenType2 {
    }

    private static final int[] layout_ids = { R.layout.used20 };
}
