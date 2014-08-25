/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.sleuthkit.autopsy.advancedtimeline.filters;

import java.util.List;
import javafx.beans.Observable;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

/**
 * A Filter with a collection of {@link Filter} sub-filters. If this
 * filter is not active than none of its sub-filters are applied either.
 * Concrete implementations can decide how to combine the sub-filters.
 *
 * a {@link CompoundFilter} uses listeners to enforce the following
 * relationships between it and its sub-filters:
 * <ol>
 * <le>if a filter becomes active, and all its sub-filters were inactive, make
 * them all active</le>
 * <le>if a filter becomes inactive and all its sub-filters were active, make
 * them all inactive</le>
 * <le>if a sub-filter changes active state set the parent filter active if any
 * of its sub-filters are active.</le>
 * </ol>
 */
public abstract class CompoundFilter extends AbstractFilter {

    /** the list of sub-filters that make up this filter */
    private final ObservableList<Filter> subFilters;

    public final ObservableList<Filter> getSubFilters() {
        return subFilters;
    }

    /** construct a compound filter from a list of other filters to combine.
     *
     * @param subFilters
     */
    public CompoundFilter(ObservableList<Filter> subFilters) {
        super();
        this.subFilters = FXCollections.<Filter>synchronizedObservableList(subFilters);

        //listen to changes in list of subfilters and add active state listener to newly added filters
        this.subFilters.addListener((ListChangeListener.Change<? extends Filter> c) -> {
            while (c.next()) {
                addListeners(c.getAddedSubList());
                //TODO: remove listeners from removed subfilters
            }
        });

        //add listeners to subfilters
        addListeners(subFilters);

        //disable subfilters if this filter is disabled
        getDisabledProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                getSubFilters().forEach((Filter t) -> {
                    t.setDisabled(true);
                });
            } else {
                final boolean isActive = !isActive();
                getSubFilters().forEach((Filter t) -> {
                    t.setDisabled(isActive);
                });
            }
        });

        //listen to active property and adjust subfilters active property
        getActiveProperty().addListener((ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) -> {
            if (newValue) {
                // if this filter become active, and all its subfilters were inactive, make them all active
                if (getSubFilters().stream().noneMatch(Filter::isActive)) {
                    getSubFilters().forEach((Filter filter) -> {
                        filter.setActive(true);
                    });
                }
            } else {
                //if this filter beceoms inactive and all its subfilters where active, make them inactive
                if (getSubFilters().stream().allMatch(Filter::isActive)) {
                    getSubFilters().forEach((Filter filter) -> {
                        filter.setActive(false);
                    });
                }
            }

            //disabled subfilters if this filter is not active
            getSubFilters().forEach((Filter t) -> {
                t.setDisabled(!newValue);
            });
        });
    }

    private void addListeners(List<? extends Filter> newSubfilters) {
        for (Filter sf : newSubfilters) {
            //if a subfilter changes active state
            sf.getActiveProperty().addListener((Observable observable) -> {
                //set this filter acttive af any of the subfilters are active.
                setActive(getSubFilters().parallelStream().anyMatch(Filter::isActive));
            });
        }
    }
}
