package com.artf.chatapp.testing;

import android.content.Context;

import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.fragment.app.Fragment;

import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasAndroidInjector;
import dagger.android.support.AndroidSupportInjection;
import dagger.internal.Beta;

@Beta
public abstract class CustomDaggerFragment extends Fragment implements HasAndroidInjector {

    @Inject
    DispatchingAndroidInjector<Object> androidInjector;

    public CustomDaggerFragment() {
        super();
    }

    @ContentView
    public CustomDaggerFragment(@LayoutRes int contentLayoutId) {
        super(contentLayoutId);
    }

    @Override
    public void onAttach(@NotNull Context context) {
        injectMembers();
        super.onAttach(context);
    }

    protected void injectMembers(){
        AndroidSupportInjection.inject(this);
    }

    @Override
    public AndroidInjector<Object> androidInjector() {
        return androidInjector;
    }
}