package com.samsung.microbit.utils;

import com.samsung.microbit.data.constants.EventCategories;
import com.samsung.microbit.data.constants.EventSubCodes;
import com.samsung.microbit.data.model.Project;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Utility that contains common functionality that
 * uses along the app.
 */
public class Utils {
    public final static int SORTBY_PROJECT_DATE = 0;
    public final static int SORTBY_PROJECT_NAME = 1;
    public final static int ORDERBY_ASCENDING = 0;
    public final static int ORDERBY_DESCENDING = 1;

    private Utils() {
    }

    /**
     * Allows to sort the list of projects on the Flash screen according
     * to given parameters. It allows to sort by date or name, for now, and
     * set sort order to ascending or descending.
     *
     * @param list List to sort.
     * @param orderBy Defines sorting criteria of a project by which to sort.
     * @param sortOrder Ascending or descending.
     * @return Sorted list.
     */
    public static List<Project> sortProjectList(List<Project> list, final int orderBy, final int sortOrder) {
        Project[] projectArray = list.toArray(new Project[list.size()]);
        Comparator<Project> comparator = new Comparator<Project>() {
            @Override
            public int compare(Project lhs, Project rhs) {
                int rc;
                switch (orderBy) {

                    case SORTBY_PROJECT_DATE:
                        // byTimestamp
                        if (lhs.timestamp < rhs.timestamp) {
                            rc = 1;
                        } else if (lhs.timestamp > rhs.timestamp) {
                            rc = -1;
                        } else {
                            rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                        }

                        break;
                    default:
                        // byName
                        rc = lhs.name.toLowerCase().compareTo(rhs.name.toLowerCase());
                        break;
                }

                if (sortOrder != ORDERBY_ASCENDING) {
                    rc = 0 - rc;
                }

                return rc;
            }
        };

        Arrays.sort(projectArray, comparator);
        list.clear();
        list.addAll(Arrays.asList(projectArray));
        return list;
    }

    /**
     * Going to and coming from microbit the following rule applies:
     * low 16 bits == event category
     * (e.g. {@link EventCategories#SAMSUNG_REMOTE_CONTROL_ID),
     * high 16 bits ==  event sub code
     * (eg. {@link EventSubCodes#SAMSUNG_REMOTE_CONTROL_EVT_PLAY)
     */
    public static int makeMicroBitValue(int category, int value) {
        return ((value << 16) | category);
    }
}
