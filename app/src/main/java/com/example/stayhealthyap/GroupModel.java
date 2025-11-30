package com.example.stayhealthyap;

public class GroupModel {
    private String groupName;
    private String icon;
    private long memberCount;

    public GroupModel() {}

    public GroupModel(String groupName, String icon, long memberCount) {
        this.groupName = groupName;
        this.icon = icon;
        this.memberCount = memberCount;
    }

    public String getGroupName() { return groupName; }
    public String getIcon() { return icon; }
    public long getMemberCount() { return memberCount; }
}