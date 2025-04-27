package com.infracomp.caso3.server;

import java.util.HashMap;
import java.util.Map;

public class ServiceTable {
    private static class Service {
        String name;
        String ip;
        int port;

        public Service(String name, String ip, int port) {
            this.name = name;
            this.ip = ip;
            this.port = port;
        }
    }

    private Map<String, Service> services;

    public ServiceTable() {
        services = new HashMap<>();
        initializeServices();
    }

    private void initializeServices() {
        // Inicializar servicios según la Figura 1
        services.put("S1", new Service("Estado vuelo", "IPS1", 1001));
        services.put("S2", new Service("Disponibilidad vuelos", "IPS2", 1002));
        services.put("S3", new Service("Costo de un vuelo", "IPS3", 1003));
    }

    public String[] getServiceInfo(String serviceId) {
        Service service = services.get(serviceId);
        if (service == null) {
            return new String[]{"-1", "-1"}; // Según requerimientos, retornar -1,-1 si no existe
        }
        return new String[]{service.ip, String.valueOf(service.port)};
    }

    public Map<String, String> getServiceList() {
        Map<String, String> serviceList = new HashMap<>();
        for (Map.Entry<String, Service> entry : services.entrySet()) {
            serviceList.put(entry.getKey(), entry.getValue().name);
        }
        return serviceList;
    }
} 