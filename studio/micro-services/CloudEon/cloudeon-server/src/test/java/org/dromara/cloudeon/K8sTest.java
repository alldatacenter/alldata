package org.dromara.cloudeon;

import org.dromara.cloudeon.dao.ClusterInfoRepository;
import org.dromara.cloudeon.utils.ByteConverter;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class K8sTest {

    @javax.annotation.Resource
    private ClusterInfoRepository clusterInfoRepository;

    @Test
    public void listNode() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        NodeList nodeList = client.nodes().list();
        List<Node> items = nodeList.getItems();
        items.forEach(e -> {
            String cpu = e.getStatus().getCapacity().get("cpu").getAmount();
            long memory = e.getStatus().getCapacity().get("memory").getNumericalAmount().longValue();
            long storage = e.getStatus().getCapacity().get("ephemeral-storage").getNumericalAmount().longValue();
            String ip = e.getStatus().getAddresses().get(0).getAddress();
            String hostname = e.getStatus().getAddresses().get(1).getAddress();
            String architecture = e.getStatus().getNodeInfo().getArchitecture();
            String containerRuntimeVersion = e.getStatus().getNodeInfo().getContainerRuntimeVersion();
            String kubeletVersion = e.getStatus().getNodeInfo().getKubeletVersion();
            String kernelVersion = e.getStatus().getNodeInfo().getKernelVersion();
            String osImage = e.getStatus().getNodeInfo().getOsImage();

            System.out.println("cpu: " + cpu);
            System.out.println("memory: " + ByteConverter.convertKBToGB(memory) + "GB");
            System.out.println("storage: " + ByteConverter.convertKBToGB(storage) + "GB");
            System.out.println("ip: " + ip);
            System.out.println("hostname: " + hostname);
            System.out.println("architecture: " + architecture);
            System.out.println("containerRuntimeVersion: " + containerRuntimeVersion);
            System.out.println("kubeletVersion: " + kubeletVersion);
            System.out.println("kernelVersion: " + kernelVersion);
            System.out.println("osImage: " + osImage);

            System.out.println("===============");

        });
    }

    @Test
    public void deployDetele() throws FileNotFoundException {
        try (KubernetesClient client = new KubernetesClientBuilder().build();) {
            client.load(new FileInputStream("/Volumes/Samsung_T5/opensource/e-mapreduce/work/k8s-resource/zookeeper13/zookeeper-server.yaml"))
                    .inNamespace("default")
                    .delete();
        }


    }

    @Test
    public void startDeploy() throws FileNotFoundException {
        KubernetesClient client = new KubernetesClientBuilder().build();
        List<HasMetadata> metadata = client.load(new FileInputStream("/Volumes/Samsung_T5/opensource/e-mapreduce/work/k8s-resource/zookeeper13/zookeeper-server.yaml"))
                .inNamespace("default")
                .create();
        String deploymentName = ((Deployment) metadata.get(0)).getMetadata().getName();
        final Deployment deployment = client.apps().deployments().inNamespace("default").withName(deploymentName).get();
        Resource<Deployment> resource = client.resource(deployment).inNamespace("default");
        resource.watch(new Watcher<Deployment>() {
            @Override
            public void eventReceived(Action action, Deployment resource) {
                log.info("{} {}", action.name(), resource.getMetadata().getName());
                switch (action) {
                    case ADDED:
                        log.info("{} got added", resource.getMetadata().getName());
                        break;
                    case DELETED:
                        log.info("{} got deleted", resource.getMetadata().getName());
                        break;
                    case MODIFIED:
                        log.info("{} got modified", resource.getMetadata().getName());
                        break;
                    default:
                        log.error("Unrecognized event: {}", action.name());
                }
            }

            @Override
            public void onClose(WatcherException cause) {
                System.out.println(cause.getMessage());
            }
        });
        resource.waitUntilReady(1200, TimeUnit.SECONDS);
    }

    @Test
    public void addNodeLabel() {
        KubernetesClient client = new KubernetesClientBuilder().build();

        // 添加label
        client.nodes().withName("fl001")
                .edit(r -> new NodeBuilder(r)
                        .editMetadata()
                        .addToLabels("my-hdfs-dn", "true")
                        .endMetadata()
                        .build());

        // 检查label
        System.out.println(client.nodes().withName("fl001").get().getMetadata().getLabels().get("my-hdfs-dn").equals("true"));
        // 移除lable
        client.nodes().withName("fl001")
                .edit(r -> new NodeBuilder(r)
                        .editMetadata()
                        .removeFromLabels("my-hdfs-dn")
                        .endMetadata()
                        .build());


    }

    @Test
    public void findPod() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        List<Pod> pods = client.pods().inNamespace("default").withLabel("app=zookeeper-server-zookeeper13").list().getItems();
        for (Pod pod : pods) {
            String nodeName = pod.getSpec().getNodeName();
            if (nodeName!=null &&nodeName.equals("fl001")) {
                // do something with the pod
                String podName = pod.getMetadata().getName();
                System.out.println(podName);
//                client.pods().withName(podName).delete();
            }
        }
    }

    @Test
    public void scale() {
        KubernetesClient client = new KubernetesClientBuilder().build();
        RollableScalableResource<Deployment> resource = client.apps().deployments().inNamespace("default").withName("zookeeper-server-zookeeper13");
        Integer readyReplicas = resource.get().getStatus().getReadyReplicas();
        System.out.println("当前deployment可用Replicas: " + readyReplicas);
        System.out.println(resource.scale(readyReplicas - 1));

    }

    @Test
    public void ip() throws UnknownHostException {
        System.out.println(InetAddress.getLocalHost().getHostAddress());
    }

    @Test
    public void kubeconfig() throws FileNotFoundException {
        String kubeConfig = clusterInfoRepository.findById(1).get().getKubeConfig();

        Config config = Config.fromKubeconfig(kubeConfig);
        KubernetesClient client = new KubernetesClientBuilder().withConfig(config).build();
        NodeList nodeList = client.nodes().list();
        List<Node> items = nodeList.getItems();
        System.out.println(items.size());
    }
}
