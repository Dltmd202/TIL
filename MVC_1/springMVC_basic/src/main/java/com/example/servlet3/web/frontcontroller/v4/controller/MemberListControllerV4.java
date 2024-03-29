package com.example.servlet3.web.frontcontroller.v4.controller;

import com.example.servlet3.domain.member.Member;
import com.example.servlet3.domain.member.MemberRepository;
import com.example.servlet3.web.frontcontroller.ModelView;
import com.example.servlet3.web.frontcontroller.v4.ControllerV4;

import java.util.List;
import java.util.Map;

public class MemberListControllerV4 implements ControllerV4 {

    private MemberRepository memberRepository = MemberRepository.getInstance();


    @Override
    public String process(Map<String, String> paramMap, Map<String, Object> model) {
        List<Member> members = memberRepository.findAll();
        ModelView mv = new ModelView("members");
        model.put("members", members);

        return "members";
    }
}