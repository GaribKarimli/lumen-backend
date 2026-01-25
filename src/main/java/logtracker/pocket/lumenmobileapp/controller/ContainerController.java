package logtracker.pocket.lumenmobileapp.controller;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Image;
import com.github.dockerjava.api.model.Network;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import logtracker.pocket.lumenmobileapp.model.ContainerInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
@Tag(name = "Containers", description = "Endpoints for managing and inspecting Docker containers")
public class ContainerController {

    private final DockerClient dockerClient;

    @Operation(summary = "List all containers", description = "Retrieves a list of all containers, including running and stopped ones.")
    @GetMapping
    public List<ContainerInfo> listContainers() {
        log.info("Fetching all containers (including stopped)");
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        return containers.stream()
                .map(this::mapToContainerInfo)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get container details", description = "Fetches detailed information about a specific container, including environment variables and port bindings.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved container details"),
            @ApiResponse(responseCode = "404", description = "Container not found")
    })
    @GetMapping("/{id}")
    public ContainerInfo getContainer(@Parameter(description = "ID of the container to inspect") @PathVariable String id) {
        log.info("Inspecting container: {}", id);
        InspectContainerResponse container = dockerClient.inspectContainerCmd(id).exec();
        
        Map<String, String> envVars = Collections.emptyMap();
        if (container.getConfig() != null && container.getConfig().getEnv() != null) {
            envVars = Arrays.stream(container.getConfig().getEnv())
                    .filter(s -> s.contains("="))
                    .map(s -> s.split("=", 2))
                    .collect(Collectors.toMap(s -> s[0], s -> s[1], (v1, v2) -> v1));
        }

        List<String> ports = Collections.emptyList();
        if (container.getNetworkSettings() != null && container.getNetworkSettings().getPorts() != null) {
            ports = container.getNetworkSettings().getPorts().getBindings().entrySet().stream()
                    .map(entry -> entry.getKey().toString() + " -> " + 
                            (entry.getValue() != null ? Arrays.toString(entry.getValue()) : "none"))
                    .collect(Collectors.toList());
        }

        List<String> mounts = Collections.emptyList();
        if (container.getMounts() != null) {
            mounts = container.getMounts().stream()
                    .map(m -> (m.getSource() != null ? m.getSource() : "") + ":" + 
                            (m.getDestination() != null ? m.getDestination() : ""))
                    .collect(Collectors.toList());
        }

        return ContainerInfo.builder()
                .id(container.getId())
                .name(container.getName().replaceFirst("/", ""))
                .status(container.getState().getStatus())
                .state(container.getState().getStatus())
                .image(container.getConfig().getImage())
                .created(0L)
                .env(envVars)
                .ports(ports)
                .mounts(mounts)
                .build();
    }

    @Operation(summary = "Start a container", description = "Starts a stopped container by its ID.")
    @PostMapping("/{id}/start")
    public void startContainer(@Parameter(description = "ID of the container to start") @PathVariable String id) {
        log.info("Starting container: {}", id);
        dockerClient.startContainerCmd(id).exec();
        log.info("Container {} started successfully", id);
    }

    @Operation(summary = "Stop a container", description = "Stops a running container by its ID.")
    @PostMapping("/{id}/stop")
    public void stopContainer(@Parameter(description = "ID of the container to stop") @PathVariable String id) {
        log.info("Stopping container: {}", id);
        dockerClient.stopContainerCmd(id).exec();
        log.info("Container {} stopped successfully", id);
    }

    @Operation(summary = "Restart a container", description = "Restarts a running or stopped container by its ID.")
    @PostMapping("/{id}/restart")
    public void restartContainer(@Parameter(description = "ID of the container to restart") @PathVariable String id) {
        log.info("Restarting container: {}", id);
        dockerClient.restartContainerCmd(id).exec();
        log.info("Container {} restarted successfully", id);
    }

    @Operation(summary = "Remove a container", description = "Forcefully removes a container by its ID.")
    @DeleteMapping("/{id}")
    public void removeContainer(@Parameter(description = "ID of the container to remove") @PathVariable String id) {
        log.info("Removing container: {}", id);
        dockerClient.removeContainerCmd(id).withForce(true).exec();
        log.info("Container {} removed successfully", id);
    }

    @Operation(summary = "List all images", description = "Retrieves a list of all Docker images available on the host.")
    @GetMapping("/images")
    public List<Image> listImages() {
        log.info("Listing Docker images");
        return dockerClient.listImagesCmd().exec();
    }

    @Operation(summary = "List all volumes", description = "Retrieves a list of all Docker volumes available on the host.")
    @GetMapping("/volumes")
    public List<com.github.dockerjava.api.command.InspectVolumeResponse> listVolumes() {
        log.info("Listing Docker volumes");
        return dockerClient.listVolumesCmd().exec().getVolumes();
    }

    @Operation(summary = "List all networks", description = "Retrieves a list of all Docker networks available on the host.")
    @GetMapping("/networks")
    public List<Network> listNetworks() {
        log.info("Listing Docker networks");
        return dockerClient.listNetworksCmd().exec();
    }

    private ContainerInfo mapToContainerInfo(Container c) {
        return ContainerInfo.builder()
                .id(c.getId())
                .name(c.getNames().length > 0 ? c.getNames()[0].replaceFirst("/", "") : c.getId())
                .status(c.getStatus())
                .state(c.getState())
                .image(c.getImage())
                .created(c.getCreated())
                .build();
    }
}
