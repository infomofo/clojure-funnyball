Moneyball? More like Funnyball amirite?
=======================================

Funnyball is a a binary classifier for predicting post-season college basketball victories based on regular season data and seeds.

Potential Data sets
---------

1. [Kaggle data](https://www.kaggle.com/c/march-machine-learning-mania/data)
2. NCAA
   1. [Rosters](http://www.ncaa.com/schools/albany-ny/basketball-men)  
   2. [Teams](http://stats.ncaa.org/team/inst_team_list?sport_code=MBB&division=1) 
   3. [Rosters](http://stats.ncaa.org/team/roster/11540?org_id=26172)

Construct a summary file for all previously known postseason victories or losses
-----------------------------------

For all matchups in previously known postseasons

    OBSERVATION(TEAM_VS), DID_WIN_IN_POSTSEASON (0,1), SEED_DIFFERENTIAL, REGULAR_SEASON_MATCHUP_WIN/LOSS, REGULAR_SEASON_STAT_DIFFS

This can be done with

1. ``(use 'funnyball.build :reload-all)``
2. ``(save-to-file results)``

Run a binary classifier on that dataset to build a model for predicting victory
---------------------------------------

1. ``(use 'funnyball.model :reload-all)``
2. ???

Profit!
-------

Run the models against the current year's regular season results and tournament seeds

1. ``(use 'funnyball.predict :reload-all)``
2. ???
