curl --cookie "urbauth-~zod=0v3.bip2n.r0k5v.1hd63.5av0t.77jh9" http://localhost:8080/~/scry/gaze/raw.txt | aws s3 cp - s3://gaze-exports/events4.txt --acl public-read --content-type 'text/plain'
