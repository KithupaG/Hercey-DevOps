package com.hercey.service;

import com.google.gson.JsonObject;
import com.hercey.entity.City;
import com.hercey.util.AppUtil;
import com.hercey.util.HibernateUtil;
import org.hibernate.Session;

import java.util.List;

public class CityService {
    public String loadAllCities(){
        JsonObject responseObject =  new JsonObject();

        Session hibernateSession = HibernateUtil.getSessionFactory().openSession();
        List<City> cityList = hibernateSession.createQuery("FROM City c", City.class).getResultList();
        responseObject.add("cities", AppUtil.GSON.toJsonTree(cityList));
        hibernateSession.close();

        return AppUtil.GSON.toJson(responseObject);
    }
}
