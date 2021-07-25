package com.android.launcher3.assistant;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.assistant.AssistFavoriteContactsRecyclerViewAdapter.ViewHolder;
import com.android.launcher3.assistant.search.SearchPhonesPopup;

import static android.support.v4.content.ContextCompat.checkSelfPermission;

/**
 * Assist page 에서 즐겨찾는 주소들을 위한 adapter
 */
public class AssistFavoriteContactsRecyclerViewAdapter extends RecyclerView.Adapter<ViewHolder> {
    Context mContext;
    Launcher mLauncher;
    LayoutInflater mInflater;
    AssistFavoriteContacts mFavoriteContacts;

    public AssistFavoriteContactsRecyclerViewAdapter(Launcher launcher, Context context, AssistFavoriteContacts favoriteContacts) {
        mLauncher = launcher;
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mFavoriteContacts = favoriteContacts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.assist_favorite_contact_item, parent,false);
        return new ViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mFavoriteContacts.mData.size();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (mFavoriteContacts.mData.get(position).avatar != null) {
            holder.mAvatar.setImageDrawable(mFavoriteContacts.mData.get(position).avatar);
            holder.mAvatar.setBackground(new ShapeDrawable(new OvalShape()));
            holder.mAvatar.setClipToOutline(true);
        } else {
            holder.mAvatar.setImageResource(R.drawable.ic_person_light_large);
            holder.mAvatar.setBackground(ContextCompat.getDrawable(mContext, R.drawable.assist_favorite_contact_avatar_touch_selector));
        }
        holder.mName.setText(mFavoriteContacts.mData.get(position).name);
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        ImageView mAvatar;
        TextView mName;

        ViewHolder(View itemView) {
            super(itemView);
            mAvatar = itemView.findViewById(R.id.item_avatar);
            mName = itemView.findViewById(R.id.item_name);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            int phoneSize = mFavoriteContacts.mData.get(getAdapterPosition()).phoneArray.size();
            if (phoneSize == 1) {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + mFavoriteContacts.mData.get(getAdapterPosition()).phoneArray.get(0).phoneNumber));
                if (checkSelfPermission(mContext,
                        android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                mLauncher.startActivity(callIntent);
            } else if (phoneSize > 1) {
                SearchPhonesPopup phonesPopup = new SearchPhonesPopup(mContext, mFavoriteContacts.mData.get(getAdapterPosition()).phoneArray,
                        mContext.getResources().getString(R.string.search_phone_popup_title_call), 1);
                phonesPopup.setGravity(2);
                phonesPopup.show();
            }
        }
    }
}


