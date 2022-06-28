package com.example.reminderalarm;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.reminderalarm.databinding.FragmentFirstBinding;

import java.util.List;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentFirstBinding.inflate(inflater, container, false);

        MainActivity mainActivity = (MainActivity) getActivity();
        // 첫 화면 진입 시 캘린더 가지는 계정 찾기
        List<CalendarCoreInfo> calendarCoreInfoList = mainActivity.calendarQuery();

        // 캘린더 가져온 후에 이벤트 가져오기
        mainActivity.eventQuery(calendarCoreInfoList);

        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
                // 캘린더 가지는 계정 찾는 쿼리
                List<CalendarCoreInfo> calendarCoreInfos = ((MainActivity) getActivity()).calendarQuery();

            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}