curl --cookie "urbauth-~zod=0v5.hnjjv.rgc2o.f2l9r.tqe1h.k7c60" http://localhost:8080/~/scry/gaze/raw.txt | aws s3 cp - s3://gaze-exports/events.txt --acl public-read --content-type 'text/plain'
