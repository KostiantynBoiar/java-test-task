package com.scrapper;
import org.camunda.bpm.engine.impl.util.json.JSONObject;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: java -jar YourApp.jar <startNodeId> <endNodeId>");
            System.exit(-1);
        }

        String startNodeId = args[0];
        String endNodeId = args[1];

        String bpmnXml = fetchBpmnXml();
        if (bpmnXml == null) {
            System.exit(-1);
        }

        BpmnModelInstance modelInstance = parseBpmnXml(bpmnXml);
        if (modelInstance == null) {
            System.exit(-1);
        }

        List<String> path = findPath(modelInstance, startNodeId, endNodeId);
        if (path == null) {
            System.exit(-1);
        }

        System.out.println("The path from " + startNodeId + " to " + endNodeId + " is:");
        System.out.println(path);
    }

    private static String fetchBpmnXml() {
        try {
            URL url = new URL("https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() != 200) {
                return null;
            }

            Scanner scanner = new Scanner(connection.getInputStream()).useDelimiter("\\A");
            String response = scanner.hasNext() ? scanner.next() : "";
            scanner.close();

            return new JSONObject(response).getString("bpmn20Xml");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static BpmnModelInstance parseBpmnXml(String bpmnXml) {
        try {
            InputStream stream = new ByteArrayInputStream(bpmnXml.getBytes());
            return Bpmn.readModelFromStream(stream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static List<String> findPath(BpmnModelInstance modelInstance, String startNodeId, String endNodeId) {
        FlowNode startNode = modelInstance.getModelElementById(startNodeId);
        FlowNode endNode = modelInstance.getModelElementById(endNodeId);

        if (startNode == null || endNode == null) {
            return null;
        }

        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();

        if (dfs(startNode, endNode, visited, path)) {
            return path;
        } else {
            return null;
        }
    }

    private static boolean dfs(FlowNode currentNode, FlowNode endNode, Set<String> visited, List<String> path) {
        visited.add(currentNode.getId());
        path.add(currentNode.getId());

        if (currentNode.equals(endNode)) {
            return true;
        }

        for (SequenceFlow outgoingFlow : currentNode.getOutgoing()) {
            FlowNode nextNode = outgoingFlow.getTarget();
            if (!visited.contains(nextNode.getId())) {
                if (dfs(nextNode, endNode, visited, path)) {
                    return true;
                }
            }
        }

        path.remove(path.size() - 1);
        visited.remove(currentNode.getId());
        return false;
    }


}