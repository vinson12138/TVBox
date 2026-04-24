package com.fongmi.android.tv.ui.custom;

import android.annotation.SuppressLint;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivitySearchBinding;
import com.fongmi.android.tv.ui.custom.CustomSearchView;
import com.fongmi.android.tv.ui.adapter.KeyboardAdapter;

import androidx.recyclerview.widget.RecyclerView;

public class CustomKeyboard implements KeyboardAdapter.OnClickListener {

    private final RecyclerView keyboard;
    private final CustomSearchView keyword;
    private final Callback callback;
    private KeyboardAdapter adapter;

    public static void init(Callback callback, ActivitySearchBinding binding) {
        new CustomKeyboard(callback, binding.keyboard, binding.keyword).initView();
    }

    public static void init(Callback callback, RecyclerView keyboard, CustomSearchView keyword) {
        new CustomKeyboard(callback, keyboard, keyword).initView();
    }

    public CustomKeyboard(Callback callback, RecyclerView keyboard, CustomSearchView keyword) {
        this.callback = callback;
        this.keyboard = keyboard;
        this.keyword = keyword;
    }

    private void initView() {
        keyboard.setItemAnimator(null);
        keyboard.setHasFixedSize(false);
        keyboard.addItemDecoration(new SpaceItemDecoration(7, 6));
        keyboard.setAdapter(adapter = new KeyboardAdapter(this));
    }

    @Override
    public void onTextClick(String text) {
        StringBuilder sb = new StringBuilder(keyword.getText().toString());
        int cursor = keyword.getSelectionStart();
        if (keyword.length() > 19) return;
        sb.insert(cursor, text);
        keyword.setText(sb.toString());
        keyword.setSelection(cursor + 1);
    }

    @Override
    @SuppressLint("NonConstantResourceId")
    public void onIconClick(int resId) {
        StringBuilder sb = new StringBuilder(keyword.getText().toString());
        int cursor = keyword.getSelectionStart();
        switch (resId) {
            case R.drawable.ic_setting_home:
                callback.showDialog();
                break;
            case R.drawable.ic_keyboard_remote:
                callback.onRemote();
                break;
            case R.drawable.ic_keyboard_search:
                callback.onSearch();
                break;
            case R.drawable.ic_keyboard_left:
                keyword.setSelection(--cursor < 0 ? 0 : cursor);
                break;
            case R.drawable.ic_keyboard_right:
                keyword.setSelection(++cursor > keyword.length() ? keyword.length() : cursor);
                break;
            case R.drawable.ic_keyboard_back:
                if (cursor == 0) return;
                sb.deleteCharAt(cursor - 1);
                keyword.setText(sb.toString());
                keyword.setSelection(cursor - 1);
                break;
            case R.drawable.ic_keyboard:
                adapter.toggle();
                break;
        }
    }

    @Override
    public boolean onLongClick(int resId) {
        if (resId != R.drawable.ic_keyboard_back) return false;
        keyword.setText("");
        return true;
    }

    public interface Callback {

        void showDialog();

        void onRemote();

        void onSearch();
    }
}
