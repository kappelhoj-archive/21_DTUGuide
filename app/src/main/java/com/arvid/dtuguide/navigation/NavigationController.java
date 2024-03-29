package com.arvid.dtuguide.navigation;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.arvid.dtuguide.activity.main.MainActivity;
import com.arvid.dtuguide.data.LocationDAO;
import com.arvid.dtuguide.data.LocationDTO;
import com.arvid.dtuguide.data.MARKTYPE;
import com.arvid.dtuguide.data.Person;
import com.arvid.dtuguide.data.Searchable;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.arvid.dtuguide.activity.main.MainActivity.TAG;

/**
 * Created by arvid on 01-11-2017.
 */

public class NavigationController implements Navigation{

    private LocationDAO dao;
    private static List<Searchable> historyList = new ArrayList<Searchable>();
    private static List<Searchable> favorite = new ArrayList<Searchable>();
    private Context context;

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference myRef = database.getReference("Searchable");

    final String HISTORYPREF = "History_list";
    final String FAVORITEPREF = "Favorite_list";

    static MainActivity ui;

    // create a reference to the shared preferences object
    SharedPreferences mySharedPreferences;
    SharedPreferences mySharedPreferencesFav;

    SharedPreferences.Editor myEditor;

    public NavigationController(){
    }

    public NavigationController(LocationDAO dao, Context context){
        this.dao = dao;
        this.context = context;

        mySharedPreferences = context.getSharedPreferences(HISTORYPREF, 0);
        mySharedPreferencesFav = context.getSharedPreferences(FAVORITEPREF, 0);

        updateDataFromFireBase();

    }

    public NavigationController(LocationDAO dao, Context context, MainActivity ui){
        this.dao = dao;
        this.context = context;
        this.ui=ui;

        mySharedPreferences = context.getSharedPreferences(HISTORYPREF, 0);
        mySharedPreferencesFav = context.getSharedPreferences(FAVORITEPREF, 0);

        updateDataFromFireBase();


    }


    public void updateDataFromFireBase(){
        myRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                HashMap<String, HashMap<String, HashMap<String, Object>>> map = (HashMap<String, HashMap<String, HashMap<String, Object>>>) dataSnapshot.getValue();

                dao.setData(new HashMap<String, Searchable>());

                HashMap<String, HashMap<String, Object>> locations = map.get("Locations");
                HashMap<String, HashMap<String, Object>> persons = map.get("Persons");

                for (HashMap<String, Object> location : locations.values()) {

                        LatLng geo = new LatLng(
                                ((HashMap<String, Double>) location.get("position")).get("latitude"),
                                ((HashMap<String, Double>) location.get("position")).get("longitude")
                        );

                    LocationDTO dto = (LocationDTO) new LocationDTO()
                            .setPosition(geo)
                            .setFloor(Integer.parseInt((String) location.get("floor")))
                            .setDescription((String) location.get("description"))
                            .setLandmark(MARKTYPE.valueOf((String) location.get("landmark")))
                            .setTags((ArrayList<String>) location.get("tags"))
                            .setName((String) location.get("name"));



                    dao.saveData(dto);
                }

                for(HashMap<String, Object> person:persons.values()){
                    Person dto = null;
                    try {
                        LocationDTO room = (LocationDTO) dao.getData((String)person.get("roomName"));

                        dto = (Person) new Person()
                                .setdescription((String)person.get("description"))
                                .setEmail((String)person.get("email"))
                                .setPictureURL((String)person.get("pictureURL"))
                                .setRole((String)person.get("role"))
                                .setRoom(room)
                                .setName((String)person.get("name"));

                        room.addPerson(dto);

                        dao.saveData(dto);
                    } catch (LocationDAO.DAOException e) {
                        e.printStackTrace();
                        System.out.println((String)person.get("name")
                                +" Object Data is rejected because the Room "
                                +(String)person.get("roomName")+" does not exist.");
                    }

                }

                try {
                    retrievePrefs();
                } catch (LocationDAO.DAOException e) {
                    e.printStackTrace();
                }
                if (ui != null) {
                    ui.generateLandmarks();

                }
            }


            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w(TAG, "Failed to read value.", error.toException());
            }
        });
    }

    private void retrievePrefs() throws LocationDAO.DAOException{
        historyList.clear();


        for(int i=0;i<mySharedPreferences.getAll().values().size();++i)
            historyList.add(dao.getData(mySharedPreferences.getString(i+"","")));

        retrieveFavorite();

    }

    private void savePrefs() {
        myEditor = mySharedPreferences.edit();
        for(int i = 0; i<historyList.size();++i){
            myEditor.putString(i+"", historyList.get(i).getName());
        }
        myEditor.commit();
    }

    private void retrieveFavorite() throws LocationDAO.DAOException{
        favorite.clear();


        for(int i=0;i<mySharedPreferencesFav.getAll().values().size();++i)
            favorite.add(dao.getData(mySharedPreferencesFav.getString(i+"","")));

    }

    private void saveFavorite(){
        myEditor = mySharedPreferencesFav.edit();
        clearFavPrefs();
        for(int i = 0; i<favorite.size();++i){
            myEditor.putString(i+"", favorite.get(i).getName());
        }
        myEditor.commit();
    }

    private void clearFavPrefs(){
        myEditor = mySharedPreferencesFav.edit();
        myEditor.clear();
        myEditor.commit();
    }

    public void clearFavorite(){
        clearFavPrefs();
        favorite.clear();
    }

    public void clearHistory(){
        myEditor = mySharedPreferences.edit();
        myEditor.clear();
        myEditor.commit();
        historyList.clear();
    }

    public void removeFavorite(Searchable itemTORemove){
        myEditor = mySharedPreferencesFav.edit();
        favorite.remove(itemTORemove);
        saveFavorite();
    }

    public void addFavorite(Searchable item){
        favorite.add(item);
        saveFavorite();
    }

    public boolean checkFavorite(Searchable item) {
        for (Searchable fav : favorite) {
            if (fav.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkHistory(Searchable item) {
        for (Searchable hist : historyList) {
            if (hist.equals(item)) {
                return true;
            }
        }
        return false;
    }

    public List<Searchable> getFavorite(){
        return favorite;
    }


    public Searchable getSearchableItem(String name) throws LocationDAO.DAOException {
        Searchable dto = dao.getData(name);

        if(historyList.contains(dto))
            historyList.remove(dto);

        else if(historyList.size()==10)
            historyList.remove(historyList.get(0));

        historyList.add(dto);
        savePrefs();
        return dto;
    }

    public List<Searchable> searchMatch(String matchString) throws LocationDAO.DAOException {
        List<Searchable> searchData = new ArrayList<Searchable>();
        matchString = matchString.replace(" ", "");

        //Search with name
        for(Searchable dto : dao.getAllData().values()){
            if(dto.getName().replace(".", "").toLowerCase().matches("(.*)"+matchString+"(.*)")
                    || dto.getName().replace(".", "").toLowerCase().matches("(.*)"+matchString+"(.*)")
                    || dto.getName().replace(" ", "").toLowerCase().matches("(.*)"+matchString+"(.*)")){
                searchData.add(dto);
            }
        }

        //If nothing is found, search with tag
        if(searchData.size()==0)
            searchData=searchWithTag(matchString);

        Collections.sort(searchData);
        return searchData;
    }

    public ArrayList<Searchable> searchWithTag(String tag) throws LocationDAO.DAOException {
        ArrayList<Searchable> tags = new ArrayList<Searchable>();

        for(Searchable dto:dao.getAllData().values()){
            if(dto.getClass().isAssignableFrom(LocationDTO.class)) {
                LocationDTO loc = (LocationDTO) dto;

                if (loc.getTags() != null) {
                    for (String t : loc.getTags()) {
                        if (t.toLowerCase().matches("(.*)" + tag + "(.*)")) {
                            tags.add(loc);
                        }
                    }
                }
            }
        }

        return tags;
    }

    public List<Searchable> getHistoryList(){
        List<Searchable> list = new ArrayList<Searchable>();

        for(Searchable item:historyList) {
            list.add(item);
        }
        Collections.reverse(list);

        return list;
    }

    public List<LocationDTO> getLandmarks() throws Exception {
        ArrayList<LocationDTO> Landmarks = new ArrayList<LocationDTO>();


        for(Searchable item:dao.getAllData().values()){

            if(item.getClass().isAssignableFrom(LocationDTO.class)){
                if(!(((LocationDTO) item).getLandmark().equals(MARKTYPE.NONE))){
                    Landmarks.add((LocationDTO) item);
                }
            }
        }
        if(Landmarks.size()>0)
            return Landmarks;
        else
            throw new Exception("No landmarks found !");
    }

}
