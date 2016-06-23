package com.samsung.microbit.utils;

import com.samsung.microbit.model.Project;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Utils {
    public final static int SORTBY_PROJECT_DATE = 0;
    public final static int SORTBY_PROJECT_NAME = 1;
    public final static int ORDERBY_ASCENDING = 0;
    public final static int ORDERBY_DESCENDING = 1;

    private Utils() {
    }

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
}
