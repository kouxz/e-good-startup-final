package com.projeto.egoodapp.data.model;

import com.projeto.egoodapp.data.local.InterestWorkflow;

public class Interest {
    public String id, dealerId, userId, vehicleId;
    public String name, phone, email, vehicleName;
    public String status = InterestWorkflow.STATUS_NEW;
    public String outcome = "";
    public long createdAt;
}
