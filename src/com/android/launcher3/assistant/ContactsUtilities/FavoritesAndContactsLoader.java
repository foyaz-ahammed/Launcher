package com.android.launcher3.assistant.ContactsUtilities;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.database.MergeCursor;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * A loader for use in the default contact list, which will also query for favorite contacts
 * if configured to do so.
 */
public class FavoritesAndContactsLoader extends CursorLoader {

    private boolean mLoadFavorites;

    private String[] mProjection;


    public FavoritesAndContactsLoader(Context context) {
        super(context);
    }

    /** Whether to load favorites and merge results in before any other results. */
    public void setLoadFavorites(boolean flag) {
        mLoadFavorites = flag;
    }

    public void setProjection(String[] projection) {
        super.setProjection(projection);
        mProjection = projection;
    }

    @Override
    public Cursor loadInBackground() {
        List<Cursor> cursors = Lists.newArrayList();
//        if (mLoadFavorites) {
//            cursors.add(loadFavoritesContacts());
//        }
        final Cursor contactsCursor = loadContacts();
        cursors.add(contactsCursor);
        return new MergeCursor(cursors.toArray(new Cursor[cursors.size()])) {
            @Override
            public Bundle getExtras() {
                // Need to get the extras from the contacts cursor.
                return contactsCursor == null ? new Bundle() : contactsCursor.getExtras();
            }
        };
    }

    private Cursor loadContacts() {
        // ContactsCursor.loadInBackground() can return null; MergeCursor
        // correctly handles null cursors.
        try {
            return super.loadInBackground();

        } catch (NullPointerException | SQLiteException | SecurityException e) {
            // Ignore NPEs, SQLiteExceptions and SecurityExceptions thrown by providers
        }
        return null;
    }

//    private Cursor loadFavoritesContacts() {
//        final StringBuilder selection = new StringBuilder();
//        selection.append(Contacts.STARRED + "=?");
//        final ContactListFilter filter =
//                ContactListFilterController.getInstance(getContext()).getFilter();
//        if (filter != null && filter.filterType == ContactListFilter.FILTER_TYPE_CUSTOM) {
//            selection.append(" AND ").append(Contacts.IN_VISIBLE_GROUP + "=1");
//        }
//        return getContext().getContentResolver().query(
//                Contacts.CONTENT_URI, mProjection, selection.toString(), new String[]{"1"},
//                getSortOrder());
//    }
}
