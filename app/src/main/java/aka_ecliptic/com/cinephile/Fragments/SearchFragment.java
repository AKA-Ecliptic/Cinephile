package aka_ecliptic.com.cinephile.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Switch;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import aka_ecliptic.com.cinephile.Activity.MovieProfileActivity;
import aka_ecliptic.com.cinephile.Adapter.RecyclerViewAdapterSearch;
import aka_ecliptic.com.cinephile.DataRepository.Repository;
import aka_ecliptic.com.cinephile.Handler.GsonMovieConverter;
import aka_ecliptic.com.cinephile.Handler.TMDBHandler;
import aka_ecliptic.com.cinephile.Model.Media;
import aka_ecliptic.com.cinephile.Model.Movie;
import aka_ecliptic.com.cinephile.R;

public class SearchFragment extends Fragment implements RecyclerViewAdapterSearch.SelectedItemListener{

    private static final String TAG = "SearchFragment";

    private Repository<Media> repository;
    private RecyclerViewAdapterSearch adapter;
    private Switch aSwitch;
    private List<Media> onlineSearch;
    private int pageCount = 1;
    private int insertAmount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_search, container, false);

        repository = new Repository<>(view.getContext());
        onlineSearch = new ArrayList<>();

        RecyclerView recyclerView = view.findViewById(R.id.searchRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this.getContext()));
        adapter = new RecyclerViewAdapterSearch(this.getContext(), repository.getItems());
        adapter.setSelectedClickListener(this);
        recyclerView.setAdapter(adapter);

        aSwitch = view.findViewById(R.id.searchSwitch);

        SearchView searchView = view.findViewById(R.id.searchSearchView);
        searchView.setOnClickListener(v -> searchView.setIconified(false));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if(aSwitch.isChecked()) {
                    if (query != null && query.length() != 0) {
                        TMDBHandler.getInstance(view.getContext()).search(query, pageCount, result -> {
                            loadSearch(result);
                            adapter.addOnlineData(onlineSearch);
                            adapter.getFilter().filter(query);
                        });
                        return false;
                    }
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                onlineSearch.clear();
                adapter.addOnlineData(onlineSearch);
                adapter.getFilter().filter(newText);
                return false;
            }
        });
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) -> {
            if(!hasFocus){
                if(searchView.getQuery() == null || searchView.getQuery().length() <= 0){
                    searchView.setIconified(true);
                }
            }
        });

        aSwitch.setOnClickListener(l -> {
            String query = searchView.getQuery().toString();
            adapter.setOnline(aSwitch.isChecked());
            if(!aSwitch.isChecked()){
                if(query.length() > 0) {
                    adapter.getFilter().filter(query);
                }
            }else{
                if(query.length() > 0) {
                    TMDBHandler.getInstance(view.getContext()).search(query,
                            pageCount, result -> {
                                loadSearch(result);
                                adapter.addOnlineData(onlineSearch);
                                adapter.getFilter().filter(query);
                            });
                }
            }
        });

        return view;
    }

    private void loadSearch(JSONObject result){
        try {
            onlineSearch.clear();
            JSONArray jsonArray = result.getJSONArray("results");
            for(insertAmount = 0; insertAmount < jsonArray.length(); insertAmount++){
                JSONObject jso = (JSONObject) jsonArray.get(insertAmount);
                Gson gson = GsonMovieConverter.getCustomGson();

                Movie m = gson.fromJson(jso.toString(), Movie.class);
                onlineSearch.add(m);
            }
        }catch (Exception e){
            e.printStackTrace();
            Log.d(TAG,"Error "+ e + "found making an API request");
            Toast.makeText(this.getContext(), "There was an error making request",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onSelect(View view, int position) {
        Bundle b = new Bundle();
        b.putSerializable(Movie.class.getName(), adapter.getItem(position));
        Intent newIntent = new Intent(view.getContext(), MovieProfileActivity.class);
        newIntent.putExtras(b);
        startActivityForResult(newIntent, 0);

        Toast.makeText(view.getContext(), "Opening " + adapter.getItem(position).getTitle(), Toast.LENGTH_SHORT).show();
    }
}
