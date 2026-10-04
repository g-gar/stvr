package com.ggar.stvr.inspection.implementation;

import com.ggar.stvr.inspection.api.InspectionChain;
import com.ggar.stvr.inspection.api.InspectionPlugin;
import com.ggar.stvr.inspection.entities.StreamInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Reactive pipeline executor that runs InspectionPlugins in order directly over StreamInfo.
 */
@Component
public class InspectionPipeline {

    private final java.util.function.Supplier<List<InspectionPlugin>> pluginsSupplier;
    private final String configuredPipeline;

    public InspectionPipeline(
            List<InspectionPlugin> availablePlugins,
            @Value("${stvr.inspection.pipeline:}") String configuredPipeline
    ) {
        Objects.requireNonNull(availablePlugins, "availablePlugins cannot be null");
        this.pluginsSupplier = () -> availablePlugins;
        this.configuredPipeline = configuredPipeline;
    }

    public InspectionPipeline(
            java.util.function.Supplier<List<InspectionPlugin>> pluginsSupplier,
            String configuredPipeline
    ) {
        this.pluginsSupplier = Objects.requireNonNull(pluginsSupplier, "pluginsSupplier cannot be null");
        this.configuredPipeline = configuredPipeline;
    }

    public Mono<StreamInfo> execute(StreamInfo initialInfo) {
        Objects.requireNonNull(initialInfo, "initialInfo cannot be null");

        List<InspectionPlugin> applicablePlugins = getOrderedPlugins().stream()
                .filter(plugin -> plugin.supports(initialInfo))
                .toList();

        return buildChain(applicablePlugins, 0).proceed(initialInfo);
    }

    public List<InspectionPlugin> getOrderedPlugins() {
        return Collections.unmodifiableList(sortPlugins(pluginsSupplier.get(), configuredPipeline));
    }

    private InspectionChain buildChain(List<InspectionPlugin> plugins, int index) {
        if (index >= plugins.size()) {
            return Mono::just;
        }
        InspectionPlugin current = plugins.get(index);
        return info -> current.inspect(info, buildChain(plugins, index + 1));
    }

    private static List<InspectionPlugin> sortPlugins(
            List<InspectionPlugin> plugins,
            String configuredPipeline
    ) {
        if (configuredPipeline == null || configuredPipeline.isBlank()) {
            return plugins.stream()
                    .sorted(Comparator.comparingInt(InspectionPlugin::getOrder))
                    .collect(Collectors.toList());
        }

        List<String> orderList = Arrays.stream(configuredPipeline.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        Map<String, InspectionPlugin> pluginMap = plugins.stream()
                .collect(Collectors.toMap(InspectionPlugin::getId, p -> p, (a, b) -> a));

        List<InspectionPlugin> sorted = new ArrayList<>();
        for (String id : orderList) {
            InspectionPlugin plugin = pluginMap.remove(id);
            if (plugin != null) {
                sorted.add(plugin);
            }
        }

        pluginMap.values().stream()
                .sorted(Comparator.comparingInt(InspectionPlugin::getOrder))
                .forEach(sorted::add);

        return sorted;
    }
}
