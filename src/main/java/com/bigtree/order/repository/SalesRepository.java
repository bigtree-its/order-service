package com.bigtree.order.repository;

import com.bigtree.order.model.FoodOrder;
import com.bigtree.order.model.SumPrice;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesRepository extends MongoRepository<FoodOrder, String> {

    @Aggregation(pipeline =
            {"{$project: {\n" +
            "        month: {$month: $dateCreated},\n" +
            "        year: {$year: $dateCreated},\n" +
            "        amount: 1,\n" +
            "        dateCreated: 1\n" +
            "     }}"
            ,"{$match: {$and : [{year:?1} , {month:?2}, { cloudKitchen._id : ?0 }]}}"
            ,"{$group: { \n" +
            "          '_id': {\n" +
            "            month: {$month: $dateCreated},\n" +
            "            year: {$year: $dateCreated} \n" +
            "          },\n" +
            "          totalPrice: {$sum: {$toDecimal:$total}},\n" +
            "          }\n" +
            "      }"
            ,"{$project: {\n" +
            "        _id: 0,\n" +
            "        totalPrice: {$toString: $totalPrice}\n" +
            "     }}"
            }
            )
    AggregationResults<SumPrice> sumPriceThisYearMonth(String cloudKitchenId, Integer year, Integer month);
}
