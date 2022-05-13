curl --cookie "urbauth-~zod=0v4.ghrop.lff2j.f5cb5.9rgi1.h1mo0" http://localhost:8080/~/scry/gaze/raw.txt | aws s3 cp - s3://gaze-exports/events.txt --acl public-read --content-type 'text/plain'
