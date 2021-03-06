package aka_ecliptic.com.cinephile.Fragment;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import aka_ecliptic.com.cinephile.Adapter.CollectionsListAdapter;
import aka_ecliptic.com.cinephile.Architecture.MediaViewModel;
import aka_ecliptic.com.cinephile.Architecture.Repository;
import aka_ecliptic.com.cinephile.MainActivity;
import aka_ecliptic.com.cinephile.Model.Movie;
import aka_ecliptic.com.cinephile.R;

import static aka_ecliptic.com.cinephile.Fragment.MyListFragment.SELECTED_MOVIE;
import static aka_ecliptic.com.cinephile.Fragment.MyListFragment.SELECTED_SAVED;

public class CollectionsFragment extends Fragment {
    static final String INSTANCE_NAME = "INSTANCE";

    private String fragName;

    private MediaViewModel mediaViewModel;
    private Repository.Sort currentSort = Repository.Sort.DEFAULT;
    private List<Movie> fragList;

    private CollectionsListAdapter adapter;
    private TextView emptyPrompt;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setUpViewModelLink();
        assignInstanceName();
        return inflater.inflate(R.layout.fragment_collections, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setUpRecycler();
    }

    @Override
    public void onResume() {
        MainActivity.setSortClickListener(() -> {
            currentSort = mediaViewModel.cycleSort(currentSort);
            fragList = mediaViewModel.sortList(fragList, currentSort);

            Toast.makeText(requireActivity(), currentSort.getSortType(), Toast.LENGTH_SHORT).show();

            adapter.setItems(fragList);
        });

        super.onResume();
    }

    private void setUpViewModelLink() {
        mediaViewModel = new ViewModelProvider(requireActivity()).get(MediaViewModel.class);
    }

    private void assignInstanceName() {
        if (getArguments() != null) {
            fragName = getArguments().getString(INSTANCE_NAME);
        }else {
            fragName = "";
        }
    }

    private void checkPrompt(){
        emptyPrompt.setVisibility((fragList.isEmpty()) ? View.VISIBLE : View.GONE);
    }

    private void setUpRecycler() {
        if(fragName.equals("All")){
            fragList = new ArrayList<>(mediaViewModel.reCacheItems());
        }else {
            fragList = mediaViewModel.getItemsInCollection(fragName);
        }

        RecyclerView recyclerView = requireView().findViewById(R.id.collections_recycler);
        adapter = new CollectionsListAdapter(requireContext(), fragList);

        emptyPrompt = requireView().findViewById(R.id.collections_text_empty_prompt);
        checkPrompt();

        setUpAdapterListeners();

        MyListFragment.addSubscriber(() -> {
            if(fragName.equals("All")){
                fragList = new ArrayList<>(mediaViewModel.reCacheItems());
            }else {
                fragList = mediaViewModel.getItemsInCollection(fragName);
            }

            adapter.setItems(fragList);
            checkPrompt();
        });

        mediaViewModel.addSubscriber((update, destructive) -> {
            int index = fragList.indexOf(update);
            if(index >= 0){
                if(destructive){
                    fragList.removeIf(m -> m.getId() == update.getId());
                    adapter.notifyItemRemoved(index);
                }else {
                    fragList.set(index, update);
                    adapter.notifyItemChanged(index);
                }
            }
            checkPrompt();
        });

        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    private void setUpAdapterListeners() {
        adapter.setClickListener((v, p) -> {
            Bundle bundle = new Bundle();

            bundle.putSerializable(SELECTED_MOVIE, adapter.getItem(p));
            bundle.putBoolean(SELECTED_SAVED, true);

            NavController navController = Navigation.findNavController(requireView());

            int origin = (navController.getCurrentBackStackEntry() != null) ?
                    navController.getCurrentBackStackEntry().getDestination().getId() :
                    R.id.mylist_fragment;

            if(origin == R.id.collections_fragment){
                navController.navigate(R.id.action_collections_fragment_to_movie_profile_fragment, bundle);
            }else {
                navController.navigate(R.id.action_mylist_fragment_to_movie_profile_fragment, bundle);
            }

            Snackbar snackbar = Snackbar.make(requireActivity().findViewById(R.id.main_coordinator),
                    "Opening '" + adapter.getItem(p).getTitle() + "'",
                    Snackbar.LENGTH_SHORT);
            snackbar.getView().setBackgroundColor(requireActivity().getColor(R.color.colorSecondaryDark));
            snackbar.show();
        });

        adapter.setCheckBoxListener((v, p) -> {
            Movie m = adapter.getItem(p);
            m.setSeen(!m.isSeen());
            mediaViewModel.updateItem(m);
            mediaViewModel.notifyClones(m, false);
        });

        adapter.setOnLongClickListener((v, p) -> {
            Movie m = adapter.getItem(p);

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = requireActivity().getLayoutInflater();
            builder.setView(inflater.inflate(R.layout.dialog_delete, (ViewGroup) requireView(), false));

            AlertDialog dialog = builder.create();
            dialog.show();

            TextView title = dialog.findViewById(R.id.delete_dialog_text_title);
            TextView movieTitle = dialog.findViewById(R.id.delete_dialog_text_target_title);

            title.setText(R.string.dialog_title_delete);
            movieTitle.setText(m.getTitle());

            Button confirm = dialog.findViewById(R.id.delete_dialog_button_confirm);
            Button cancel = dialog.findViewById(R.id.delete_dialog_button_cancel);

            confirm.setOnClickListener(view -> {
                mediaViewModel.deleteItem(m);
                fragList.remove(m);
                adapter.notifyItemRemoved(p);
                mediaViewModel.notifyClones(m, true);

                dialog.dismiss();
            });

            cancel.setOnClickListener(view -> dialog.cancel());

            return true;
        });
    }
}
