package logtracker.pocket.dozzlemobileapp.controller;

import com.github.docker-java.api.DockerClient;
import com.github.docker-java.api.model.Container;
import logtracker.pocket.dozzlemobileapp.model.ContainerInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/containers")
@RequiredArgsConstructor
public class ContainerController {

    private final DockerClient dockerClient;

    @GetMapping
    public List<ContainerInfo> listContainers() {
        List<Container> containers = dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec();

        return containers.stream()
                .map(c -> ContainerInfo.builder()
                        .id(c.getId())
                        .name(c.getNames().length > 0 ? c.getNames()[0].replaceFirst("/", "") : c.getId())
                        .status(c.getState())
                        .build())
                .collect(Collectors.toList());
    }
}
