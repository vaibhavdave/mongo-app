# 08 - Text & Geo Search

Module 04 introduced text indexes. This module briefly recaps that, then
spends most of its time on **geospatial** queries - "which stores are
near here" and "which stores are inside this shape" - backed by a
`2dsphere` index over GeoJSON points.

## Concepts covered

### GeoJSON points and the `2dsphere` index

```java
@GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
private GeoJsonPoint location;
```

A `GeoJsonPoint` stores `[longitude, latitude]` - **in that order**. Almost
every map UI shows you "lat, lng", so this is the single most common
mistake when working with MongoDB geodata; get it backwards and your
points end up in the wrong hemisphere. The `2dsphere` index tells MongoDB
to treat this field as a point on a sphere (the Earth), enabling accurate
great-circle distance calculations rather than flat-plane math.

### `$geoNear` - nearest first, with distance

```java
NearQuery query = NearQuery.near(new Point(lng, lat), Metrics.KILOMETERS)
    .maxDistance(new Distance(maxDistanceKm, Metrics.KILOMETERS))
    .spherical(true);
GeoResults<Store> results = mongoTemplate.geoNear(query, Store.class);
```

`$geoNear` (via `mongoTemplate.geoNear`) returns matches **sorted by
distance**, with the actual distance to each one attached
(`GeoResult.getDistance()`). That per-result distance is what sets it
apart from a plain filter - see below.

### `$geoWithin` - a plain membership test

```java
Circle circle = new Circle(new Point(lng, lat), new Distance(radiusKm, Metrics.KILOMETERS));
Query query = query(where("location").withinSphere(circle));
```

`$geoWithin` (`.withinSphere()` for a circle, `.within()` for an arbitrary
GeoJSON polygon) just answers "is this point inside this shape" - no
distance, no automatic sort-by-proximity. It's the right tool when you
only need a yes/no filter and don't care how close within the boundary
each match is; it doesn't need to compute and sort by distance the way
`$geoNear` does.

### Polygon queries

`findWithinPolygon()` takes an arbitrary list of `[lng, lat]` points
(the ring must close - first point equals last) and finds every store
inside that shape - useful for "within this delivery zone" or "within this
drawn map region" style queries, not just simple radius circles.

## Running it

```bash
cd 08-text-geo-search
docker compose up -d
./mvnw spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080):

1. Add several stores - vary their coordinates so some cluster together
   and others are far apart (nearby city coordinates work well for a
   visible spread on the map).
2. Set a center point and distance, run **$geoNear** - the highlighted
   (red) dots are matches, and the JSON output shows each one's distance,
   nearest first.
3. Run **$geoWithin (circle)** with the same center/distance - compare:
   same matching set, but no distance or ordering guarantee in the
   response.
4. Try the text search recap with a word from a store's name or
   description.

The SVG panel is a rough local projection (longitude scaled by
`cos(latitude)` so it's visually proportional near your plotted area) -
just a visual aid; the actual query math on the server is exact spherical
geometry, not this approximation.

## REST API

| Method | Path | Notes |
|---|---|---|
| `POST` | `/api/stores` | Create (`{name, description, category, lng, lat}`) |
| `GET` | `/api/stores` | List all |
| `GET` | `/api/stores/near?lng=&lat=&maxDistanceKm=` | `$geoNear`, sorted, with distance |
| `GET` | `/api/stores/within-radius?lng=&lat=&radiusKm=` | `$geoWithin` circle |
| `POST` | `/api/stores/within-polygon` | `$geoWithin` polygon (`{points: [[lng,lat], ...]}`) |
| `GET` | `/api/stores/search/text?q=` | `$text` search, sorted by relevance |

## Things to try

1. In Mongo Express, open a store document and look at the stored
   `location` shape (`{"type": "Point", "coordinates": [lng, lat]}`) -
   that's the actual GeoJSON MongoDB stores and indexes.
2. Add a store exactly at your query center with `maxDistanceKm` very
   small - confirm it still matches (distance ~0).
3. Call `/api/stores/within-polygon` with a manually-drawn triangle or
   rectangle around a subset of your stores and confirm only those inside
   the shape come back.
4. Compare `$geoNear` and `$geoWithin` with `explain()` (module 04's
   technique) - see how both use the 2dsphere index rather than scanning
   every store.

## What's next

Module 09 covers replication: running a real multi-node MongoDB replica
set, read preferences, write concerns, and what happens to the app during
a simulated primary failover.
