package com.arvid.dtuguide;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.arvid.dtuguide.data.Searchable;

import java.util.List;

/**
 * Created by Jeppe on 14-01-2018.
 */

public class FavoritesAdapter extends RecyclerView.Adapter<RecViewHolder>{

    private List<Searchable> mList;
    private int layout;
    private Context context;

    public FavoritesAdapter(Context context, List<Searchable> mList, int layout){
        this.mList = mList;
        this.layout = layout;
        this.context = context;
    }

    public int getItemCount(){
        if (mList != null)
            return mList.size();
        else
            return 0;
    }

    public RecViewHolder onCreateViewHolder(ViewGroup viewGroup, int position){

        View v = LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup, false);
        RecViewHolder pvh = new RecViewHolder(v, R.id.recycler_item_fav_name, R.id.recycler_item_fav_type, R.id.recycler_item_fav_icon);
        return pvh;
    }

    public void onBindViewHolder(RecViewHolder holder, final int i){
        holder.text.setText(mList.get(i).getName());
        holder.text2.setText(mList.get(i).getType());
        switch (mList.get(i).getType()) {
            case "Person":
                holder.image.setImageResource(R.drawable.ic_perm_identity_black_24dp);
                break;
            case "Location":
                holder.image.setImageResource(R.drawable.ic_place_black_24dp);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: open main activity and zoom to the location
                Toast.makeText(context, "clicked item " + i , Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }
}
