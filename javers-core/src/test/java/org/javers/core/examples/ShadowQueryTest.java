package org.javers.core.examples;

import static org.junit.Assert.assertEquals;

import org.javers.core.CommitIdGenerator;
import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.examples.model.Employee;
import org.javers.repository.jql.JqlQuery;
import org.javers.repository.jql.QueryBuilder;
import org.javers.shadow.Shadow;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author Gleb Bondarchuk
 * Created on 04, 22 2018.
 */
public class ShadowQueryTest {

    @Test
    public void shadowQueryTest() {
        Javers javers = JaversBuilder
                .javers()
                .withCommitIdGenerator(CommitIdGenerator.RANDOM)
                .build();

        Employee ryan = new Employee("Ryan", 1000);
        Map<String, String> properties1 = new HashMap<>();
        properties1.put("1", "1");
        javers.commit("author", ryan, properties1);

        ryan.setSalary(2000);
        Map<String, String> properties2 = new HashMap<>();
        properties2.put("2", "2");
        javers.commit("author", ryan, properties2);

        JqlQuery query = QueryBuilder.byInstanceId("Ryan", Employee.class)
                .withChildValueObjects()
                .withNewObjectChanges()
                .skip(0)
                .limit(6)
                .withScopeDeepPlus(1000)
                .build();

        List<Shadow<Employee>> shadows = javers.findShadows(query);
        assertEquals(2, shadows.size());
    }
}
