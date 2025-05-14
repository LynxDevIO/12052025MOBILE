package com.example.manipulararquivos;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.ArrayList;
import java.util.List;

public class Fragments extends FragmentStateAdapter {
    List<Fragment> lista = new ArrayList<>();
    List<String> nomes = new ArrayList<>();

    public Fragments(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void addFragment(Fragment fragment, String text) {
        lista.add(fragment);
        nomes.add(text);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return lista.get(position);
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }
}
