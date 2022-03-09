curl --cookie "urbauth-~zod=0v5.hv8om.uc1m6.iditk.phm5g.vmi19" http://localhost:80/~/scry/gaze/raw.txt | aws s3 cp - s3://gaze-exports/events.txt --acl public-read --content-type 'text/plain'
