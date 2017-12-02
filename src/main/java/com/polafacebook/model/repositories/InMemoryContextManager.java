package com.polafacebook.model.repositories;

import com.polafacebook.model.Context;

import java.util.ArrayList;
import java.util.List;

public class InMemoryContextManager implements ContextManager {
    private List<Context> contextList = new ArrayList<>();

    @Override
    public boolean saveContext(Context context) {
        return true;
    }

    @Override
    public Context getContext(String id) {
        for (Context c : contextList) {
            if (c.userId.equals(id)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Context getOrCreateContext(String id) {
        Context c = getContext(id);
        if(c != null){
            return c;
        }
        c = new Context(id);
        contextList.add(c);
        return c;
    }

    @Override
    public boolean deleteContext(String currentId) {
        contextList.remove(getContext(currentId));
        return true;
    }
}
