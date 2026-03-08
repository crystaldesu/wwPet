package com.wwpet.model;

import java.util.ArrayList;
import java.util.List;

public final class SpeechLines {
    private List<String> firstLaunch = new ArrayList<>(List.of(
            "你好，我是 wwPet，以后一起专注吧。"
    ));
    private List<String> restInteractions = new ArrayList<>(List.of(
            "摸摸头，我今天也会陪着你。",
            "休息也很重要呀。",
            "我已经准备好继续陪你啦。"
    ));
    private List<String> focusInteractions = new ArrayList<>(List.of(
            "继续冲，我在旁边给你打气。",
            "专注中的你超厉害。",
            "再坚持一下，我们离目标更近啦。"
    ));
    private List<String> snapToTaskbar = new ArrayList<>(List.of(
            "我已经稳稳停靠在任务栏边上啦。"
    ));
    private List<String> focusStart = new ArrayList<>(List.of(
            "进入专注状态，我会安静陪着你。"
    ));
    private List<String> timedFocusStart = new ArrayList<>(List.of(
            "专注计时开始啦，我们一起认真一会儿。"
    ));
    private List<String> focusSwitchToRest = new ArrayList<>(List.of(
            "从专注切回休息状态啦，辛苦啦。"
    ));
    private List<String> focusExitEarly = new ArrayList<>(List.of(
            "提前退出专注也没关系，我们稍后再继续。"
    ));
    private List<String> focusTimedComplete = new ArrayList<>(List.of(
            "专注计时完成，先休息一下吧。"
    ));
    private List<String> focusMilestone = new ArrayList<>(List.of(
            "我们累计专注 {hours} 小时啦。"
    ));

    public SpeechLines copy() {
        SpeechLines copy = new SpeechLines();
        copy.firstLaunch = new ArrayList<>(firstLaunch);
        copy.restInteractions = new ArrayList<>(restInteractions);
        copy.focusInteractions = new ArrayList<>(focusInteractions);
        copy.snapToTaskbar = new ArrayList<>(snapToTaskbar);
        copy.focusStart = new ArrayList<>(focusStart);
        copy.timedFocusStart = new ArrayList<>(timedFocusStart);
        copy.focusSwitchToRest = new ArrayList<>(focusSwitchToRest);
        copy.focusExitEarly = new ArrayList<>(focusExitEarly);
        copy.focusTimedComplete = new ArrayList<>(focusTimedComplete);
        copy.focusMilestone = new ArrayList<>(focusMilestone);
        return copy;
    }

    public List<String> getFirstLaunch() {
        return firstLaunch;
    }

    public void setFirstLaunch(List<String> firstLaunch) {
        this.firstLaunch = sanitize(firstLaunch);
    }

    public List<String> getRestInteractions() {
        return restInteractions;
    }

    public void setRestInteractions(List<String> restInteractions) {
        this.restInteractions = sanitize(restInteractions);
    }

    public List<String> getFocusInteractions() {
        return focusInteractions;
    }

    public void setFocusInteractions(List<String> focusInteractions) {
        this.focusInteractions = sanitize(focusInteractions);
    }

    public List<String> getSnapToTaskbar() {
        return snapToTaskbar;
    }

    public void setSnapToTaskbar(List<String> snapToTaskbar) {
        this.snapToTaskbar = sanitize(snapToTaskbar);
    }

    public List<String> getFocusStart() {
        return focusStart;
    }

    public void setFocusStart(List<String> focusStart) {
        this.focusStart = sanitize(focusStart);
    }

    public List<String> getTimedFocusStart() {
        return timedFocusStart;
    }

    public void setTimedFocusStart(List<String> timedFocusStart) {
        this.timedFocusStart = sanitize(timedFocusStart);
    }

    public List<String> getFocusSwitchToRest() {
        return focusSwitchToRest;
    }

    public void setFocusSwitchToRest(List<String> focusSwitchToRest) {
        this.focusSwitchToRest = sanitize(focusSwitchToRest);
    }

    public List<String> getFocusExitEarly() {
        return focusExitEarly;
    }

    public void setFocusExitEarly(List<String> focusExitEarly) {
        this.focusExitEarly = sanitize(focusExitEarly);
    }

    public List<String> getFocusTimedComplete() {
        return focusTimedComplete;
    }

    public void setFocusTimedComplete(List<String> focusTimedComplete) {
        this.focusTimedComplete = sanitize(focusTimedComplete);
    }

    public List<String> getFocusMilestone() {
        return focusMilestone;
    }

    public void setFocusMilestone(List<String> focusMilestone) {
        this.focusMilestone = sanitize(focusMilestone);
    }

    private List<String> sanitize(List<String> values) {
        List<String> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (String value : values) {
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }
}
