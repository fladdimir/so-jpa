package org.demo.sql_array;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import javax.transaction.Transactional;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.jdbc.SqlConfig.ErrorMode;
import org.springframework.test.context.jdbc.SqlConfig.TransactionMode;

@Sql(statements = SqlArrayTest.SETUP_STRING, executionPhase = ExecutionPhase.BEFORE_TEST_METHOD, //
        config = @SqlConfig(transactionMode = TransactionMode.ISOLATED, errorMode = ErrorMode.FAIL_ON_ERROR))
@SpringBootTest
class SqlArrayTest {

    @Autowired
    private SqlArrayEntityRepository repository;

    static final String SETUP_STRING = "" //
            + "DELETE FROM sql_array_entity_longs;" //
            + "DELETE FROM sql_array_entity;" //
            // insert 2 rows, with 1 and 2 value(s) in the array
            + "INSERT INTO sql_array_entity (id, ints, strings) VALUES (1, ARRAY[2,3], ARRAY['s1']), (2, ARRAY[4], ARRAY['s2', 's3']);" //
            // insert 2 longs for the first entity
            + "INSERT INTO sql_array_entity_longs (sql_array_entity_id, longs, longs_order) VALUES (1, 4, 0), (1, 6, 1);";

    @Transactional
    @Rollback(false)
    @Test
    void testSqlSetup() {

        var builder = new BooleanBuilder();
        builder.and(
                Expressions.booleanTemplate("function('array_length', {0}, 1)", QSqlArrayEntity.sqlArrayEntity.ints)
                        .castToNum(Integer.class).gt(1));
        builder.and(QSqlArrayEntity.sqlArrayEntity.longs.size().goe(2));

        Iterable<SqlArrayEntity> results = repository.findAll(builder);
        assertThat(results).hasSize(1);
        var result = results.iterator().next();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getInts()).containsExactly(2, 3);
        assertThat(result.getLongs()).containsExactly(4L, 6L);
    }

    @Transactional
    @Rollback(false)
    @Test
    void testStrings() {
        assertThat(repository.findAll().stream().map(SqlArrayEntity::getStrings).flatMap(Arrays::stream))
                .containsExactlyInAnyOrder("s1", "s2", "s3");
    }

}
