package app.search;

import app.model.SearchEvent;

import java.util.List;

@FunctionalInterface
public interface SearchListener {

    void onEvents(List<SearchEvent> events);

    default void onEvent(SearchEvent event) {
        onEvents(List.of(event));
    }
}
