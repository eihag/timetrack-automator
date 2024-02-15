package com.timetrack;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(TimeTrackNativeRuntimeHints.class)
public class TimeTrackConfiguration {
}
