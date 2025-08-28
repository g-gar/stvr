package com.ggar.streamlink.mapper;

import com.ggar.streamlink.model.dto.Platform;
import com.ggar.streamlink.model.dto.PlatformDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlatformMapper {
    PlatformDTO toDto(Platform platform);
}
