package com.nh.shorturl.dto.request.history;

import com.nh.shorturl.type.GroupingType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class RedirectionStatsRequest {
    private List<GroupingType> groupBy;
}
