package com.univoice.chat.service;

import com.univoice.chat.dto.GroupDtos.*;

import java.util.List;

public interface GroupService {

    GroupSummaryDto createGroup(String creatorUuid, CreateGroupRequest request);

    List<GroupSummaryDto> getUserGroups(String userUuid);

    GroupSummaryDto getGroupDetails(String requesterUuid, String groupUuid);

    GroupSummaryDto addMembers(String requesterUuid, String groupUuid, AddMembersRequest request);

    void removeMember(String requesterUuid, String groupUuid, String targetUserUuid);

    GroupSummaryDto updateMemberLanguage(String userUuid, String groupUuid, String targetLanguage);
}